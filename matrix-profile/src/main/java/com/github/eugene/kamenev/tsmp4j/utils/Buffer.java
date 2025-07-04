/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.eugene.kamenev.tsmp4j.utils;

import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Buffer Abstraction class
 */
public abstract class Buffer {

    protected final int mSize;
    protected int mCnt = 0;
    protected int mStart = 0;
    protected int mEnd = 0;

    public Buffer(int mSize) {
        this.mSize = mSize;
    }

    public void init() {
        mCnt = 0;
        mStart = 0;
        mEnd = 0;
    }

    public int size() {
        return Math.min(mCnt, mSize);
    }

    public boolean isFull() {
        return mCnt >= mSize;
    }

    public int getLength() {
        return mSize;
    }


    public static class ObjBuffer<T> extends Buffer {

        private final T[] buff;

        public ObjBuffer(T[] buff) {
            this(buff, false);
        }

        public ObjBuffer(T[] buff, boolean isFull) {
            super(buff.length);
            this.buff = buff;
            if (isFull) {
                this.mCnt = buff.length;
                this.mEnd = this.mCnt - 1;
            }
        }

        public void addToEnd(T val) {
            if (mCnt < mSize) {
                buff[mCnt] = val;
                mEnd = mCnt;
            } else {
                mEnd = (mEnd + 1) % mSize;
                mStart = (mStart + 1) % mSize;
                buff[mEnd] = val;
            }
            mCnt++;
        }

        public T head() {
            return this.get(0);
        }

        public T tail() {
            return this.get(Math.min(mCnt, mSize) - 1);
        }

        public Stream<T> toStream() {
            return IntStream.range(0, Math.min(mCnt, mSize))
                .mapToObj(this::get);
        }

        public Stream<T> toStreamReversed() {
            int to = Math.min(mCnt, mSize);
            int from = 0;
            return IntStream
                .iterate(to - 1, i -> i - 1)
                .limit(to - from)
                .mapToObj(this::get);
        }

        public T get(final int i) {
            if (isFull()) {
                if (i < mSize) {
                    int ix = (mStart + i) % mSize;
                    return buff[ix];
                } else {
                    throw new BufferOverflowException();
                }
            } else {
                return buff[i];
            }
        }

        /**
         * Get reversed
         *
         * @param i index back
         * @return 0.0 in case if value is missing
         */
        public T getR(final int i) {
            final int t = Math.min(mCnt, mSize) - 1;
            if (t < 0 || i > t) {
                return null;
            }
            return get(Math.max(0, t - i));
        }

        public void copy(T[] copyArray) {
            for (int i = 0; i < copyArray.length; i++) {
                var value = get(i);
                if (value.getClass().isArray() && copyArray[i] != null) {
                    System.arraycopy(value, 0, copyArray[i], 0, Array.getLength(copyArray[i]));
                } else {
                    copyArray[i] = value;
                }
            }
        }
    }

    public static class DoubleBuffer extends Buffer {

        private final double[] buff;

        public DoubleBuffer(final int size) {
            super(size);
            this.buff = new double[size];
        }

        public DoubleBuffer(DoubleBuffer buffer) {
            super(buffer.size());
            this.buff = buffer.copy();
            this.mCnt = this.buff.length;
            this.mEnd = this.mCnt - 1;
        }

        public void addToEnd(double val) {
            if (mCnt < mSize) {
                buff[mCnt] = val;
                mEnd = mCnt;
            } else {
                mEnd = (mEnd + 1) % mSize;
                mStart = (mStart + 1) % mSize;
                buff[mEnd] = val;
            }
            mCnt++;
        }

        public double head() {
            return this.get(0);
        }

        public double tail() {
            return this.get(Math.min(mCnt, mSize) - 1);
        }

        public DoubleStream toStream() {
            return IntStream.range(0, Math.min(mCnt, mSize))
                .mapToDouble(this::get);
        }

        public DoubleStream toStreamReversed() {
            int to = Math.min(mCnt, mSize);
            int from = 0;
            return IntStream
                .iterate(to - 1, i -> i - 1)
                .limit(to - from)
                .mapToDouble(this::get);
        }

        public double get(final int i) {
            if (isFull()) {
                if (i < mSize) {
                    int ix = (mStart + i) % mSize;
                    return buff[ix];
                } else {
                    throw new BufferOverflowException();
                }
            } else {
                return buff[i];
            }
        }

        /**
         * Get reversed
         *
         * @param i index back
         * @return 0.0 in case if value is missing
         */
        public double getR(final int i) {
            final int t = Math.min(mCnt, mSize) - 1;
            if (t < 0 || i > t) {
                return 0.0;
            }
            return get(Math.max(0, t - i));
        }

        public void copy(double[] copyArray) {
            for (int i = 0; i < copyArray.length; i++) {
                copyArray[i] = get(i);
            }
        }

        public double[] copy() {
            double[] copyArray = new double[Math.min(mCnt, mSize)];
            copy(copyArray);
            return copyArray;
        }
    }

    public static class IntBuffer extends Buffer {

        private final int[] buff;

        public IntBuffer(final int size) {
            super(size);
            this.buff = new int[size];
        }

        public void addToEnd(int val) {
            if (mCnt < mSize) {
                buff[mCnt] = val;
                mEnd = mCnt;
            } else {
                mEnd = (mEnd + 1) % mSize;
                mStart = (mStart + 1) % mSize;
                buff[mEnd] = val;
            }
            mCnt++;
        }

        public int head() {
            return this.get(0);
        }

        public int tail() {
            return this.get(Math.min(mCnt, mSize) - 1);
        }

        public IntStream toStream() {
            return IntStream.range(0, Math.min(mCnt, mSize))
                .map(this::get);
        }

        public IntStream toStreamReversed() {
            int to = Math.min(mCnt, mSize);
            int from = 0;
            return IntStream
                .iterate(to - 1, i -> i - 1)
                .limit(to - from)
                .map(this::get);
        }

        public int get(final int i) {
            if (isFull()) {
                if (i < mSize) {
                    int ix = (mStart + i) % mSize;
                    return buff[ix];
                } else {
                    throw new BufferOverflowException();
                }
            } else {
                return buff[i];
            }
        }

        /**
         * Get reversed
         *
         * @param i index back
         * @return 0.0 in case if value is missing
         */
        public int getR(final int i) {
            final int t = Math.min(mCnt, mSize) - 1;
            if (t < 0 || i > t) {
                return 0;
            }
            return get(Math.max(0, t - i));
        }

        public void copy(int[] copyArray) {
            for (int i = 0; i < copyArray.length; i++) {
                copyArray[i] = get(i);
            }
        }

        public int[] copy() {
            int[] copyArray = new int[Math.min(mCnt, mSize)];
            copy(copyArray);
            return copyArray;
        }
    }
}
