package com.badoo.hprof.viewer;

import com.badoo.hprof.library.HprofReader;
import com.badoo.hprof.library.Tag;
import com.badoo.hprof.library.heap.HeapDumpProcessor;
import com.badoo.hprof.library.heap.HeapDumpReader;
import com.badoo.hprof.library.heap.HeapTag;
import com.badoo.hprof.library.heap.processor.HeapDumpDiscardProcessor;
import com.badoo.hprof.library.model.ClassDefinition;
import com.badoo.hprof.library.model.HprofString;
import com.badoo.hprof.library.model.Instance;
import com.badoo.hprof.library.processor.DiscardProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * HPROF processor for finding data (classes definitions and instances) of Views and ViewGroups
 */
public class ViewDataProcessor extends DiscardProcessor {

    private Map<Integer, HprofString> strings = new HashMap<Integer, HprofString>();
    private Map<Integer, ClassDefinition> classes = new HashMap<Integer, ClassDefinition>();
    private List<Instance> instances = new ArrayList<Instance>();
    private HeapDumpDiscardProcessor heapDumpProcessor = new HeapDumpDiscardProcessor() {

        @Override
        public void onHeapRecord(int tag, @Nonnull HeapDumpReader reader) throws IOException {
            switch (tag) {
                case HeapTag.CLASS_DUMP: {
                    reader.readClassDumpRecord(classes);
                    break;
                }
                case HeapTag.INSTANCE_DUMP: {
                    instances.add(reader.readInstanceDump());
                    break;
                }
                default:
                    super.onHeapRecord(tag, reader);
            }
        }
    };

    public Map<Integer, ClassDefinition> getClasses() {
        return classes;
    }

    public Map<Integer, HprofString> getStrings() {
        return strings;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    @Override
    public void onRecord(int tag, int timestamp, int length, @Nonnull HprofReader reader) throws IOException {
        switch (tag) {
            case Tag.STRING: {
                HprofString string = reader.readStringRecord(length, timestamp);
                strings.put(string.getId(), string);
                break;
            }
            case Tag.LOAD_CLASS: {
                ClassDefinition cls = reader.readLoadClassRecord();
                classes.put(cls.getObjectId(), cls);
                break;
            }
            case Tag.HEAP_DUMP:
            case Tag.HEAP_DUMP_SEGMENT: {
                HeapDumpReader heapReader = new HeapDumpReader(reader.getInputStream(), length, heapDumpProcessor);
                while (heapReader.hasNext()) {
                    heapReader.next();
                }
                break;
            }
            default:
                super.onRecord(tag, timestamp, length, reader);
        }
    }
}
