package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.*;

public class SamplerCache implements AutoCloseable {
    private final boolean simple;
    private GpuSampler[] cache;

    public SamplerCache(GpuDevice device, boolean simple) {
        this.simple = simple;
        this.cache = new GpuSampler[simple ? 4 : 16];

        if (simple) {
            for (GpuAddressMode address : GpuAddressMode.values()) {
                for (GpuFilterMode filter : GpuFilterMode.values()) {
                    int idx = address.ordinal() << 1 | filter.ordinal();
                    this.cache[idx] = device.createSampler(null, address, address, filter, filter);
                }
            }
        } else {
            for (GpuAddressMode addressU : GpuAddressMode.values()) {
                for (GpuAddressMode addressV : GpuAddressMode.values()) {
                    for (GpuFilterMode minFilter : GpuFilterMode.values()) {
                        for (GpuFilterMode magFilter : GpuFilterMode.values()) {
                            int idx = addressU.ordinal() << 3 | addressV.ordinal() << 2 | minFilter.ordinal() << 1 | magFilter.ordinal();
                            this.cache[idx] = device.createSampler(null, addressU, addressV, minFilter, magFilter);
                        }
                    }
                }
            }
        }
    }

    public GpuSampler get(GpuAddressMode address, GpuFilterMode filter) {
        int key = this.simple ? address.ordinal() << 1 | filter.ordinal() : address.ordinal() << 3 | address.ordinal() << 2 | filter.ordinal() << 1 | filter.ordinal();
        return this.cache[key];
    }

    public GpuSampler get(GpuAddressMode addressU, GpuAddressMode addressV, GpuFilterMode minFilter, GpuFilterMode magFilter) {
        int key = this.simple ? addressU.ordinal() << 1 | minFilter.ordinal() : addressU.ordinal() << 3 | addressV.ordinal() << 2 | minFilter.ordinal() << 1 | magFilter.ordinal();
        return this.cache[key];
    }

    @Override
    public void close() {
        for (GpuSampler sampler: this.cache) {
            sampler.close();
        }
        this.cache = new GpuSampler[0];
    }
}
