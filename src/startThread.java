import java.util.TimerTask;
import java.util.Timer;

class startThread extends TimerTask {
    public void run() {
      // System.out.println("Hello World!"); 
    	imageProcess ip = new imageProcess();
    	try {
			ip.imageProc();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
 //}



public static void main(String[] args){
 Timer timer = new Timer();
 timer.schedule(new startThread(), 0, 6000);
}

}