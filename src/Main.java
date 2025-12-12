public class Main {
    public static void main(String[] args) {
        // 게임의 핵심 로직을 관리하는 컨트롤러를 생성합니다.
        GameController controller = new GameController();
        // 컨트롤러를 통해 게임 클라이언트와 UI를 초기화하고 실행합니다.
        controller.start();
    }
}
