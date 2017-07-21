package com.dotcms.api.system.event.local;

/**
 * Just for testing
 */
public class TestDelegateSubscriber implements EventSubscriber<TestEventType2> {

    private boolean called = false;



    public boolean isCalled() {
        return called;
    }


    @Override
    public void notify(TestEventType2 event) {

        System.out.println(event.getMsg());
        called = true;
    }
}
