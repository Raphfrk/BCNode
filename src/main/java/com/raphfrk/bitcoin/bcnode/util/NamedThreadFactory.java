/*
 * This file is part of BCNode.
 *
 * Copyright (c) Raphfrk 2013 <www.raphfrk.com/bcnode>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.raphfrk.bitcoin.bcnode.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final boolean daemon;
    private static final AtomicInteger idCounter = new AtomicInteger();

    public NamedThreadFactory(String namePrefix) {
            this(namePrefix, false);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Executor{" + namePrefix + "-" + idCounter.getAndIncrement() + "}");
            thread.setDaemon(daemon);
            return thread;
    }
}

