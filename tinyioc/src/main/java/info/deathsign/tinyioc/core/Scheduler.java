package info.deathsign.tinyioc.core;

import info.deathsign.tinyioc.annotation.AutoInject;
import info.deathsign.tinyioc.annotation.AutoInvoke;
import info.deathsign.tinyioc.annotation.AutoManage;
import info.deathsign.tinyioc.annotation.AutoRun;
import info.deathsign.tinyioc.misc.Tuple;
import info.deathsign.tinyioc.util.TimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoManage
public class Scheduler {
    Thread loop;

    @AutoInject
    IOCManager iocManager;

    @AutoInvoke
    private void init() {
        loop = new Thread(() -> {
            this.runner();
        });
        loop.start();
    }

    private void runner() {
        HashMap<Object, Long> timer = new HashMap<>();
        List<Tuple<Object, Method, AutoRun>> runner = iocManager.getInstanceByMethodAnnotation(AutoRun.class);
        while (true) {
            runner.forEach(x -> {
                Long aLong = timer.get(x.get1());
                if (aLong == null) {
                    aLong = TimeUtils.getTimestamp();
                    timer.put(x.get1(), aLong);
                }

                long delta = TimeUtils.getTimestamp() - aLong.longValue();
                if (delta >= x.get3().tick()) {
                    try {
                        x.get2().invoke(x.get1());

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    TimeUnit.MICROSECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
