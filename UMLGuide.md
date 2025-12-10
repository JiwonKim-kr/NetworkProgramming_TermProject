# 십이장기 게임 프로젝트 UML 가이드

이 문서는 십이장기 게임 프로젝트의 주요 클래스들과 그들 간의 관계를 UML(Unified Modeling Language) 관점에서 설명합니다.

## 1. 주요 클래스 및 역할

*   **`Main`**: 애플리케이션의 진입점. `GameController`를 초기화하고 게임을 시작합니다.
*   **`GameController`**: 클라이언트 측의 핵심 로직을 담당합니다. UI와 `GameClient` 사이의 중재자 역할을 하며, 사용자 입력 처리 및 서버 통신을 관리합니다.
*   **`GameUI`**: 전체 UI를 관리하는 최상위 클래스. `CardLayout`을 사용하여 `LobbyPanel`, `RoomPanel`, `ReplayPanel` 간 전환을 담당합니다.
*   **`LobbyPanel`**: 로비 화면을 구성하는 패널. 방 목록 표시, 방 생성/참가, 로비 채팅 기능을 제공합니다.
*   **`RoomPanel`**: 게임 방 화면을 구성하는 패널. `BoardPanel`, `ChatPanel`을 포함하며 게임 상태 및 플레이어 상태를 표시합니다.
*   **`BoardPanel`**: 게임 보드를 시각적으로 표현하고 사용자 입력을 처리하는 패널. 기물 이미지, 이동 가능 범위 하이라이트 등을 담당합니다.
*   **`ChatPanel`**: 채팅 메시지를 표시하고 입력받는 패널.
*   **`CreateRoomDialogPanel`**: 방 생성 시 필요한 정보를 입력받는 다이얼로그 패널.
*   **`GameClient`**: 서버와의 네트워크 통신을 담당하는 클라이언트. 서버로 메시지를 보내고 서버로부터 메시지를 수신하여 `GameController`에 전달합니다.
*   **`Server`**: 게임 서버의 메인 클래스. 클라이언트 연결을 수락하고, `ClientHandler`를 관리하며, 게임방(`GameRoom`) 생성 및 관리를 담당합니다.
*   **`ClientHandler`**: 서버 측에서 개별 클라이언트와의 통신을 처리하는 스레드. 클라이언트의 요청을 파싱하고 적절한 게임 로직을 호출합니다. `PlayerConnection` 인터페이스를 구현합니다.
*   **`PlayerConnection`**: 클라이언트와의 연결을 추상화하는 인터페이스. `ClientHandler`가 이를 구현하여 게임 로직이 네트워크 구현에 직접 의존하지 않도록 합니다.
*   **`GameRoom`**: 하나의 게임방을 나타내는 클래스. 호스트, 게스트, 관전자 `ClientHandler`를 관리하고, `GameSession`을 포함합니다.
*   **`GameSession`**: 실제 게임의 진행 로직을 담당하는 클래스. `GameLogic`을 사용하여 게임 상태를 관리하고, 플레이어의 명령을 처리합니다.
*   **`GameLogic`**: 게임의 규칙과 상태 변화를 관리하는 핵심 로직 클래스. `GameBoard`를 포함하며, 기물 이동, 잡은 말 놓기, 게임 종료 조건 등을 처리합니다.
*   **`GameBoard`**: 게임 보드의 현재 상태(기물 배치, 잡은 말 목록)를 나타내는 클래스. 기물 이동 및 배치 유효성을 검사합니다.
*   **`Piece`**: 게임 기물의 종류, 소유자, 이동 전략을 정의하는 Enum.
*   **`MoveStrategy`**: 기물의 이동 규칙을 정의하는 인터페이스. 전략 패턴을 적용하여 다양한 기물의 이동 방식을 캡슐화합니다.
*   **`DefaultMoveStrategy`**: `MoveStrategy`를 구현하여 일반적인 방향으로 한 칸 이동하는 기물(왕, 장, 상, 후)의 이동 규칙을 제공합니다.
*   **`PawnMoveStrategy`**: `MoveStrategy`를 구현하여 '자'(Pawn) 기물의 특수한 이동 규칙(앞으로 한 칸)을 제공합니다.
*   **`ReplayPanel`**: 저장된 리플레이 파일을 로드하고 재생하는 패널.
*   **`Command`**: (현재 코드에서는 직접적인 구현체가 사용되지 않지만) 커맨드 패턴을 위한 함수형 인터페이스.
*   **`Protocol`**: 클라이언트와 서버 간 통신에 사용되는 명령어 및 상수들을 정의한 유틸리티 클래스.

## 2. 클래스 다이어그램 (텍스트 기반 표현)

```
+-----------------+       +-----------------+       +-----------------+
|      Main       |       |  GameController |       |      GameUI     |
+-----------------+       +-----------------+       +-----------------+
| + main()        |-----> | + start()       |<----->| + createAndShow()|
+-----------------+       | + onNickname... |       | + showLobby()   |
                          | + createRoom()  |       | + enterRoom()   |
                          | + joinRoom()    |       | + showReplay()  |
                          | + sendChat()    |       | + showError()   |
                          | + onBoardClicked()|     | + updateRoomList()|
                          | + handleServerMsg()|    | + appendChatMsg()|
                          +-----------------+       +-----------------+
                                  ^   ^                   ^   ^   ^
                                  |   |                   |   |   |
                                  |   |                   |   |   |
                                  |   |                   |   |   |
                                  |   |                   |   |   |
                                  |   |                   |   |   |
+-----------------+       +-----------------+       +-----------------+
|   GameClient    |<------|   LobbyPanel    |<----->|    RoomPanel    |
+-----------------+       +-----------------+       +-----------------+
| - socket        |       | - lobbyButtons  |       | - boardPanel    |
| - in, out       |       | - userListModel |       | - chatPanel     |
| - nickname      |       | + updateRoomList()|     | + updatePlayerStatus()|
| + start()       |       | + updateUserList()|     | + updateGameState()|
| + sendMessage() |       | + appendChatMsg()|      | + handleGameStart()|
+-----------------+       +-----------------+       | + handleGameOver()|
                                  ^                   | + resetRoomUI() |
                                  |                   +-----------------+
                                  |                           ^   ^
                                  |                           |   |
                                  |                           |   |
                                  |                           |   |
                                  |                           |   |
                                  |                           |   |
+-------------------------+       |                   +-----------------+
| CreateRoomDialogPanel   |       |                   |    BoardPanel   |
+-------------------------+       |                   +-----------------+
| - roomTitleField        |       |                   | - boardButtons  |
| - passwordField         |       |                   | - boardState    |
| + getRoomTitle()        |       |                   | + updateBoard() |
| + getPassword()         |       |                   | + updateCapturedPieces()|
| + getMaxPlayers()       |       |                   | + highlightValidMoves()|
+-------------------------+       |                   | + highlightSelected...()|
                                  |                   +-----------------+
                                  |                           ^
                                  |                           |
                                  |                           |
                                  |                           |
                                  |                           |
                                  |                           |
+-----------------+       +-----------------+       +-----------------+
|     Server      |       | ClientHandler   |       |   ChatPanel     |
+-----------------+       +-----------------+       +-----------------+
| - clients       |-----> | - clientSocket  |       | + appendChatMsg()|
| - gameRooms     |       | - out, in       |       | + clearChat()   |
| + main()        |       | - nickname      |       +-----------------+
| + broadcast...()|       | - currentRoom   |
| + createGameRoom()|     | + run()         |
| + joinGameRoom()|       | + sendMessage() |
| + removeGameRoom()|     | + getNickname() |
+-----------------+       | + getCurrentRoom()|
        ^                 | + setCurrentRoom()|
        |                 +-----------------+
        |                           ^
        |                           | implements
        |                           |
+-----------------+       +-----------------+
|    GameRoom     |<------| PlayerConnection|
+-----------------+       +-----------------+
| - title         |       | + sendMessage() |
| - password      |       | + getNickname() |
| - maxPlayers    |       +-----------------+
| - host, guest   |
| - spectators    |
| - currentSession|
| + addPlayer()   |
| + removePlayer()|
| + handlePlayerCommand()|
| + broadcast...()|
+-----------------+
        ^
        |
        |
+-----------------+       +-----------------+       +-----------------+
|   GameSession   |-----> |    GameLogic    |-----> |    GameBoard    |
+-----------------+       +-----------------+       +-----------------+
| - gameRoom      |       | - board         |       | - board[][]     |
| - gameLogic     |       | - currentPlayer |       | - p1Captured    |
| - host, guest   |       | - gameState     |       | - p2Captured    |
| - player1, player2|     | - moveHistory   |       | + setupInitialBoard()|
| + processCommand()|     | + startGame()   |       | + getPieceAt()  |
| + startGame()   |       | + handleMove()  |       | + movePiece()   |
| + handleMove...()|      | + handlePlace() |       | + placeCapturedPiece()|
| + broadcastState()|     | + executeMove() |       | + getValidMoves()|
| + saveReplay()  |       | + undoLastMove()|       | + findPiece()   |
+-----------------+       | + getBoard()    |       | + isValid()     |
                          +-----------------+       +-----------------+
                                  ^                           ^
                                  |                           |
                                  |                           |
                                  |                           |
                                  |                           |
                                  |                           |
+-----------------+       +-----------------+       +-----------------+
|      Piece      |-----> |  MoveStrategy   |<------| ReplayPanel     |
+-----------------+       +-----------------+       +-----------------+
| - displayName   |       | + getValidMoves()|      | - replayLogic   |
| - owner         |       +-----------------+       | - moveNotations |
| - moveStrategy  |               ^                 | + loadReplay()  |
| + getDisplayName()|             | implements      | + previousMove()|
| + getOwner()    |             |                 | + nextMove()    |
| + getMoveStrategy()|          |                 | + refreshReplayView()|
| + promote()     |             |                 +-----------------+
| + demote()      |             |
| + flipOwner()   |             |
+-----------------+             |
                                |
+-------------------------+     +-------------------------+
| DefaultMoveStrategy     |     | PawnMoveStrategy        |
+-------------------------+     +-------------------------+
| - directions[][]        |     | + getValidMoves()       |
| + getValidMoves()       |     +-------------------------+
+-------------------------+

+-----------------+
|    Protocol     |
+-----------------+
| + CREATE_ROOM   |
| + JOIN_ROOM     |
| ... (constants) |
+-----------------+
```

## 3. 관계 설명

### 3.1. 상속 및 구현 (Inheritance / Realization)

*   **`ClientHandler` --|> `PlayerConnection`**: `ClientHandler`는 `PlayerConnection` 인터페이스를 구현하여 클라이언트 연결의 추상화된 기능을 제공합니다.
*   **`DefaultMoveStrategy` --|> `MoveStrategy`**: `DefaultMoveStrategy`는 `MoveStrategy` 인터페이스를 구현하여 일반적인 기물 이동 로직을 정의합니다.
*   **`PawnMoveStrategy` --|> `MoveStrategy`**: `PawnMoveStrategy`는 `MoveStrategy` 인터페이스를 구현하여 '자' 기물의 특수한 이동 로직을 제공합니다.

### 3.2. 합성 (Composition)

합성은 한 객체가 다른 객체의 생명 주기를 전적으로 관리하는 강한 포함 관계를 나타냅니다.

*   **`GameUI` --\* `LobbyPanel`**: `GameUI`가 `LobbyPanel`을 생성하고 관리합니다.
*   **`GameUI` --\* `RoomPanel`**: `GameUI`가 `RoomPanel`을 생성하고 관리합니다.
*   **`GameUI` --\* `ReplayPanel`**: `GameUI`가 `ReplayPanel`을 생성하고 관리합니다.
*   **`RoomPanel` --\* `BoardPanel`**: `RoomPanel`이 `BoardPanel`을 생성하고 관리합니다.
*   **`RoomPanel` --\* `ChatPanel`**: `RoomPanel`이 `ChatPanel`을 생성하고 관리합니다.
*   **`GameRoom` --\* `GameSession`**: `GameRoom`이 `GameSession`을 생성하고 게임 진행 동안 관리합니다.
*   **`GameSession` --\* `GameLogic`**: `GameSession`이 `GameLogic`을 생성하고 게임 로직을 위임합니다.
*   **`GameLogic` --\* `GameBoard`**: `GameLogic`이 `GameBoard`를 생성하고 게임 보드 상태를 관리합니다.
*   **`Piece` --\* `MoveStrategy`**: `Piece` Enum의 각 기물은 자신의 `MoveStrategy` 인스턴스를 직접 가집니다.

### 3.3. 연관 (Association)

연관은 두 클래스 간의 구조적인 관계를 나타내며, 한 클래스가 다른 클래스의 인스턴스를 멤버 변수로 가지는 경우입니다. 생명 주기가 독립적일 수 있습니다.

*   **`GameController` -- `GameUI`**: `GameController`는 `GameUI` 인스턴스를 참조하여 UI를 조작합니다.
*   **`GameController` -- `GameClient`**: `GameController`는 `GameClient` 인스턴스를 참조하여 서버와 통신합니다.
*   **`Server` -- `ClientHandler`**: `Server`는 연결된 `ClientHandler` 인스턴스들을 관리합니다.
*   **`Server` -- `GameRoom`**: `Server`는 생성된 `GameRoom` 인스턴스들을 관리합니다.
*   **`ClientHandler` -- `GameRoom`**: `ClientHandler`는 자신이 현재 속한 `GameRoom`을 참조합니다.
*   **`GameSession` -- `GameRoom`**: `GameSession`은 자신이 속한 `GameRoom`을 참조합니다.
*   **`GameSession` -- `ClientHandler`**: `GameSession`은 게임에 참여하는 `ClientHandler` (player1, player2)를 참조합니다.
*   **`ReplayPanel` -- `GameLogic`**: `ReplayPanel`은 리플레이 재생을 위해 `GameLogic` 인스턴스를 사용합니다.

### 3.4. 의존 (Dependency)

의존은 한 클래스가 다른 클래스를 사용하지만, 멤버 변수로 유지하지는 않는 약한 관계를 나타냅니다. 주로 메서드 호출, 파라미터 전달, 지역 변수 사용 등의 경우입니다.

*   **`Main` -> `GameController`**: `Main`은 `GameController`를 생성하고 메서드를 호출합니다.
*   **`GameUI` -> `GameController`**: `GameUI`는 사용자 이벤트를 `GameController`의 메서드로 전달합니다.
*   **`LobbyPanel` -> `GameController`**: `LobbyPanel`은 사용자 이벤트를 `GameController`의 메서드로 전달합니다.
*   **`LobbyPanel` -> `CreateRoomDialogPanel`**: `LobbyPanel`은 `CreateRoomDialogPanel` 객체를 생성하여 사용합니다.
*   **`RoomPanel` -> `GameController`**: `RoomPanel`은 사용자 이벤트를 `GameController`의 메서드로 전달합니다.
*   **`BoardPanel` -> `GameController`**: `BoardPanel`은 사용자 이벤트를 `GameController`의 메서드로 전달합니다.
*   **`ChatPanel` -> `GameController`**: `ChatPanel`은 채팅 메시지를 `GameController`의 메서드로 전달합니다.
*   **`ClientHandler` -> `Server`**: `ClientHandler`는 `Server`의 정적 메서드를 호출합니다.
*   **`GameRoom` -> `Server`**: `GameRoom`은 `Server`의 정적 메서드를 호출합니다.
*   **`GameRoom` -> `GameLogic`**: `GameRoom`은 `GameLogic`의 `GameState` Enum을 사용합니다.
*   **`GameSession` -> `Server`**: `GameSession`은 `Server`의 정적 메서드를 호출합니다.
*   **`GameSession` -> `Piece`**: `GameSession`은 `Piece` Enum을 사용합니다.
*   **`GameLogic` -> `Piece`**: `GameLogic`은 `Piece` Enum을 사용하여 기물을 식별하고 조작합니다.
*   **`GameBoard` -> `Piece`**: `GameBoard`는 `Piece` Enum을 사용하여 보드 위의 기물을 나타냅니다.
*   **`GameBoard` -> `MoveStrategy`**: `GameBoard`는 `MoveStrategy` 인터페이스를 통해 기물의 이동 가능 범위를 질의합니다.
*   **`ReplayPanel` -> `GameController`**: `ReplayPanel`은 로비로 돌아가기 위해 `GameController`의 메서드를 호출합니다.
*   **`ReplayPanel` -> `GameUI`**: `ReplayPanel`은 `GameUI`의 메서드를 호출하여 화면 전환을 요청합니다.
*   **모든 클래스 -> `Protocol`**: 대부분의 클래스는 `Protocol` 클래스에 정의된 상수들을 사용하여 통신 명령을 정의합니다.
