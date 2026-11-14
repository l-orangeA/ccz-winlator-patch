package com.winlator.alsaserver;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.winlator.core.KeyValueSet;
import com.winlator.math.Mathf;
import com.winlator.sysvshm.SysVSharedMemory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class ALSAClient {
    private static short framesPerBuffer = 256;
    private int bufferCapacity;
    private int bufferSize;
    private byte frameBytes;
    protected final Options options;
    private int position;
    private ByteBuffer sharedBuffer;
    private DataType dataType = DataType.U8;
    private AudioTrack audioTrack = null;
    private byte channels = 2;
    private int sampleRate = 0;
    private short previousUnderrunCount = 0;

    /* loaded from: classes.dex */
    public enum DataType {
        U8(1),
        S16LE(2),
        S16BE(2),
        FLOATLE(4),
        FLOATBE(4);

        public final byte byteCount;

        DataType(int byteCount) {
            this.byteCount = (byte) byteCount;
        }
    }

    /* loaded from: classes.dex */
    public static class Options {
        public short latencyMillis = 40;
        public byte performanceMode = 1;
        public float volume = 1.0f;

        public static Options fromKeyValueSet(KeyValueSet config) {
            if (config == null) {
                return new Options();
            }
            Options options = new Options();
            String str = config.get("performanceMode");
            char c = 65535;
            switch (str.hashCode()) {
                case 48:
                    if (str.equals("0")) {
                        c = 0;
                        break;
                    }
                    break;
                case 49:
                    if (str.equals("1")) {
                        c = 1;
                        break;
                    }
                    break;
                case 50:
                    if (str.equals("2")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    options.performanceMode = (byte) 0;
                    break;
                case 1:
                    options.performanceMode = (byte) 1;
                    break;
                case 2:
                    options.performanceMode = (byte) 2;
                    break;
            }
            options.volume = config.getFloat("volume", 1.0f);
            options.latencyMillis = (short) config.getInt("latencyMillis", 40);
            return options;
        }
    }

    public ALSAClient(Options options) {
        this.options = options;
    }

    public void release() {
        ByteBuffer byteBuffer = this.sharedBuffer;
        if (byteBuffer != null) {
            SysVSharedMemory.unmapSHMSegment(byteBuffer, byteBuffer.capacity());
            this.sharedBuffer = null;
        }
        AudioTrack audioTrack = this.audioTrack;
        if (audioTrack != null) {
            audioTrack.pause();
            this.audioTrack.flush();
            this.audioTrack.release();
            this.audioTrack = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.alsaserver.ALSAClient$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType;

        static {
            int[] iArr = new int[DataType.values().length];
            $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType = iArr;
            try {
                iArr[DataType.U8.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType[DataType.S16LE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType[DataType.S16BE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType[DataType.FLOATLE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$winlator$alsaserver$ALSAClient$DataType[DataType.FLOATBE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public static int getPCMEncoding(DataType dataType) {
        switch (AnonymousClass1.$SwitchMap$com$winlator$alsaserver$ALSAClient$DataType[dataType.ordinal()]) {
            case 1:
                return 3;
            case 2:
            case 3:
                return 2;
            case 4:
            case 5:
                return 4;
            default:
                return 1;
        }
    }

    public static int getChannelConfig(int channels) {
        return channels <= 1 ? 4 : 12;
    }

    public void prepare() {
        this.position = 0;
        this.previousUnderrunCount = (short) 0;
        this.frameBytes = (byte) (this.channels * this.dataType.byteCount);
        release();
        if (isValidBufferSize()) {
            AudioFormat format = new AudioFormat.Builder().setEncoding(getPCMEncoding(this.dataType)).setSampleRate(this.sampleRate).setChannelMask(getChannelConfig(this.channels)).build();
            AudioTrack build = new AudioTrack.Builder().setPerformanceMode(this.options.performanceMode).setAudioFormat(format).setBufferSizeInBytes(getBufferSizeInBytes()).build();
            this.audioTrack = build;
            this.bufferCapacity = build.getBufferCapacityInFrames();
            float f = this.options.volume;
            if (f != 1.0f) {
                this.audioTrack.setVolume(f);
            }
            this.audioTrack.play();
        }
    }

    public void start() {
        AudioTrack audioTrack = this.audioTrack;
        if (audioTrack != null && audioTrack.getPlayState() != 3) {
            this.audioTrack.play();
        }
    }

    public void stop() {
        AudioTrack audioTrack = this.audioTrack;
        if (audioTrack != null) {
            audioTrack.stop();
            this.audioTrack.flush();
        }
    }

    public void pause() {
        AudioTrack audioTrack = this.audioTrack;
        if (audioTrack != null) {
            audioTrack.pause();
        }
    }

    public void drain() {
        AudioTrack audioTrack = this.audioTrack;
        if (audioTrack != null) {
            audioTrack.flush();
        }
    }

    public void writeDataToTrack(ByteBuffer data) {
        DataType dataType = this.dataType;
        if (dataType == DataType.S16LE || dataType == DataType.FLOATLE) {
            data.order(ByteOrder.LITTLE_ENDIAN);
        } else if (dataType == DataType.S16BE || dataType == DataType.FLOATBE) {
            data.order(ByteOrder.BIG_ENDIAN);
        }
        if (this.audioTrack != null) {
            data.position(0);
            do {
                try {
                    int bytesWritten = this.audioTrack.write(data, data.remaining(), 0);
                    if (bytesWritten < 0) {
                        break;
                    } else {
                        increaseBufferSizeIfUnderrunOccurs();
                    }
                } catch (Exception e) {
                }
            } while (data.position() != data.limit());
            this.position += data.position();
            data.rewind();
        }
    }

    private void increaseBufferSizeIfUnderrunOccurs() {
        int i;
        int underrunCount = this.audioTrack.getUnderrunCount();
        if (underrunCount > this.previousUnderrunCount && (i = this.bufferSize) < this.bufferCapacity) {
            this.previousUnderrunCount = (short) underrunCount;
            int i2 = i + framesPerBuffer;
            this.bufferSize = i2;
            this.audioTrack.setBufferSizeInFrames(i2);
        }
    }

    public int pointer() {
        if (this.audioTrack != null) {
            return this.position / this.frameBytes;
        }
        return 0;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public void setChannels(int channels) {
        this.channels = (byte) channels;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public ByteBuffer getSharedBuffer() {
        return this.sharedBuffer;
    }

    public void setSharedBuffer(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
    }

    public int getBufferSizeInBytes() {
        return this.bufferSize * this.frameBytes;
    }

    public static int latencyMillisToBufferSize(int latencyMillis, int channels, DataType dataType, int sampleRate) {
        byte frameBytes = (byte) (dataType.byteCount * channels);
        int bufferSize = (int) Mathf.roundTo((latencyMillis * sampleRate) / 1000.0f, framesPerBuffer, false);
        return bufferSize * frameBytes;
    }

    private boolean isValidBufferSize() {
        int i = this.bufferSize;
        return i % this.frameBytes == 0 && i > 0;
    }

    public static void assignFramesPerBuffer(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService("audio");
            String framesPerBufferStr = am.getProperty("android.media.property.OUTPUT_FRAMES_PER_BUFFER");
            short parseShort = Short.parseShort(framesPerBufferStr);
            framesPerBuffer = parseShort;
            if (parseShort == 0) {
                framesPerBuffer = (short) 256;
            }
        } catch (Exception e) {
            framesPerBuffer = (short) 256;
        }
    }
}
