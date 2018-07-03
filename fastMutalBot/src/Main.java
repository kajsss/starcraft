public class Main {

    // 봇 프로그램을 실행합니다
    public static void main(String[] args) {
        try {
            new MyBotModule().run();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}