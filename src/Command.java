/**
 * GameSession에서 실행될 모든 명령어에 대한 단일 인터페이스입니다.
 * 커맨드 패턴을 적용하여 각 명령어의 실행 로직을 캡슐화합니다.
 */
@FunctionalInterface
public interface Command {
    /**
     * 명령어를 실행합니다.
     */
    void execute();
}
