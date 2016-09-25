package com.eclipsesource.v8;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ConcurrentV8Test {

    public static class Foo {
        final int val;

        public Foo(int val) {
            this.val = val;
        }

        public int getThing() {
            return 3344;
        }

        public int getVal() {
            return val;
        }

        public void whine() throws Exception {
            throw new Exception("Whaaa!");
        }
    }

    int temp = 0;

    @Test
    public void shouldShareV8AcrossThreads() {
        final ConcurrentV8 v8 = new ConcurrentV8();

        Thread thread1 = new Thread(new Runnable() {
            @Override public void run() {
                v8.runQuietly(new ConcurrentV8Runnable() {
                        @Override
                        public void run(V8 v8) {
                            v8.executeVoidScript("var i = 3000;");
                        }
            });
            }
        });

        thread1.start();
        try {
            thread1.join();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        Thread thread2 = new Thread(new Runnable() {
            @Override public void run() {
                v8.runQuietly(new ConcurrentV8Runnable() {
                        @Override public void run(V8 v8) {
                            v8.executeVoidScript("i += 344;");
                        }
                    });
            }
        });

        thread2.start();
        try {
            thread2.join();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                Assert.assertEquals(3344, v8.executeIntegerScript("i"));
            }
        });

        v8.release();
    }

    @Test
    public void shouldShareInjectedObjectsAndClassesAcrossThreads() {
        ConcurrentV8 v8 = new ConcurrentV8();

        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                V8JavaAdapter.injectClass(Foo.class, v8);
            }
        });

        temp = 0;
        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                temp = v8.executeIntegerScript("var x = new Foo(30).$release(); x.getThing();");
            }
        });
        Assert.assertEquals(3344, temp);

        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                V8JavaAdapter.injectObject("fooey", new Foo(9001), v8);
            }
        });

        temp = 0;
        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                temp = v8.executeIntegerScript("fooey.getVal();");
            }
        });
        Assert.assertEquals(9001, temp);

        v8.release();
    }

    @Test
    public void shouldHandleExceptionsLoudlyAndQuietly() {
        ConcurrentV8 v8 = new ConcurrentV8();

        v8.runQuietly(new ConcurrentV8Runnable() {
            @Override public void run(V8 v8) throws Exception {
                V8JavaAdapter.injectClass(Foo.class, v8);
            }
        });

        try {
            v8.run(new ConcurrentV8Runnable() {
                @Override public void run(V8 v8) throws Exception {
                    v8.executeScript("var x = new Foo(33).$release(); x.whine();");
                }
            });

            Assert.fail("Regular concurrent V8 invocations should pass on exceptions.");
        } catch (Throwable e) { }

        try {
            v8.runQuietly(new ConcurrentV8Runnable() {
                @Override public void run(V8 v8) throws Exception {
                    v8.executeScript("var x = new Foo(33).$release(); x.whine();");
                }
            });
        } catch (Throwable e) {
            Assert.fail("Quiet concurrent V8 invocations should suppress exceptions.");
        }

        v8.release();
    }
}