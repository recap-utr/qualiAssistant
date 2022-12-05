package utils;

import java.lang.management.ManagementFactory;

/**
 * Class contains method to check current maximum available number of threads on the system via brute force (code mostly by stackoverflow)
 */
public class ThreadChecker {
    private static Object s = new Object();
    public static int count = 0;
    public static void checkThreadNumber(){
        for(;;){
            if(count % 5000 == 0){
                long freeram = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreeMemorySize();
                System.out.println(freeram);
                if(freeram < 2000000000L){ //this leaves roughly 1.5GB RAM left
                    break;
                }
            }
            new Thread(new Runnable(){
                public void run(){
                    synchronized(s){
                        count += 1;
                        System.err.println("New thread #"+count);
                    }
                    for(;;){
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e){
                            System.err.println(e);
                        }
                    }
                }
            }).start();
        }
    }
}
