package org.snake.controller;

import org.snake.model.FieldData;
import org.snake.model.Player;
import org.snake.model.SnakeFodder;
import org.snake.dto.ChatMessageDTO;
import org.snake.model.SnakeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snake")
public class SnakeController {
    private List<SnakeModel> snakeModels = new ArrayList<>();
    private SnakeFodder snakeFodder = new SnakeFodder();
    private List<Player> players = new ArrayList<>();
    Player bestPlayer;

    private FieldData[][] Spielfeld = new FieldData[100][60];
    private boolean[] playerAlife = new boolean[1000];
    private int anzPlayer = 0;
    private boolean isRunning = false;
    private int kästchenGröße = 10, bestScoreOfAll = 0;
    private int bestScore = 0, idBestPlayer;

    @Autowired
    private SimpMessagingTemplate webSocket;

    private void createFields() {
        snakeModels = new ArrayList<>();
        snakeFodder = new SnakeFodder();
        players = new ArrayList<>();
        playerAlife = new boolean[1000];
    }

    private void deathCounter(String playerName, SnakeModel newSnake) {
        for (Player p : players) {
            if (p.getName().equals(playerName)) {
                p.setDeaths(p.getDeaths() + 1);
                newSnake.setPlayerDeaths(p.getDeaths());

                System.out.println("Tode " + p.getDeaths());
                return;
            }
        }

        // Player doesn´t exists in the list --> newPlayer
        Player newPlayer = new Player();
        newPlayer.setName(playerName);
        newPlayer.setDeaths(0);
        players.add(newPlayer);
    }

    @PostMapping("/newPlayer")
    public ResponseEntity<SnakeModel> insertPlayerIntoGame(@RequestParam String playerName) {

        playerName = playerName.replace("xHashTagx", "#");
        String[] splitString = playerName.split("#");

        String playerColor = "notSet";
        if (splitString.length > 1) {
            playerName = splitString[0];
            playerColor = "#" + splitString[1];
        }

        System.out.println("New Player joined Game: " + playerName);

        if (anzPlayer == 0) {
            // If firstPlayer Variables initialize
            createFields();
        } else {
            // Message to all clients of position of fodder
            webSocket.convertAndSend("/snake/fodderOfSnake", snakeFodder);
        }

        SnakeModel newSnake = new SnakeModel();
        newSnake.setPlayerName(playerName);

        snakeModels.add(newSnake);
        snakeModels.get(anzPlayer).setClient(UUID.randomUUID());
        snakeModels.get(anzPlayer).setPlayerNr(anzPlayer);
        deathCounter(playerName, newSnake);

        playerAlife[anzPlayer] = true;
        snakeModels.get(anzPlayer).newSnake(0, anzPlayer * 10 % 600, playerColor);

        return ResponseEntity.ok(snakeModels.get(anzPlayer++));
    }

    @GetMapping("/runGame")
    public void runGame() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        snakeFodder.setNewPosition();
        webSocket.convertAndSend("/snake/fodderOfSnake", snakeFodder);

        setFeldArray();
        long time;

        while (true) {
            // reset Game
            if (!isRunning) {
                return;
            }

            time = System.currentTimeMillis();

            // TODO Steps flüssige Bewegung
            webSocket.convertAndSend("/snake/changeDofP", snakeModels);

            boolean exit = false;
            for (int i = 0; i < anzPlayer; i++) {

                if (!snakeModels.get(i).getPlayerAlife()) {
                    // is Dead
                } else if (snakeModels.get(i).getDirection().equals("u")) {
                    snakeModels.get(i).addPosY(kästchenGröße);
                    snakeModels.get(i).addPosX(0);

                } else if (snakeModels.get(i).getDirection().equals("o")) {
                    snakeModels.get(i).addPosY(-kästchenGröße);
                    snakeModels.get(i).addPosX(0);

                } else if (snakeModels.get(i).getDirection().equals("l")) {
                    snakeModels.get(i).addPosX(-kästchenGröße);
                    snakeModels.get(i).addPosY(0);

                } else if (snakeModels.get(i).getDirection().equals("r")) {
                    snakeModels.get(i).addPosX(kästchenGröße);
                    snakeModels.get(i).addPosY(0);
                }
                setSnakePositionOnField(i);
            }

  /*
            for (int i = 0; i < snakeModels.size() && i >= 0; i++) {
                if (snakeModels.get(i).getPlayerAlife()) {
                    for (int xy = 0; xy < snakeModels.get(i).getPosX().size() &&
                            snakeModels.get(i).getPosX().size() == snakeModels.get(i).getPosY().size(); xy++) {
                        if (Spielfeld[snakeModels.get(i).getPosX().get(xy)/100][snakeModels.get(i).getPosY().get(xy)/100].getSnakeModels().size() > 1) {

                            if (!snakeModels.get(i).reduceScore()) {
                                //snakeModels.remove(i);
                                snakeModels.get(i).setPlayerAlife(false);
                                //anzPlayer--;
                                webSocket.convertAndSend("/snake/deleted",
                                        "{\"" + "deletedPlayer\": " + i + "}");
                                break;
                            }

                            snakeModels.get(
                                    Spielfeld[snakeModels.get(i).getPosX().get(xy)]
                                            [snakeModels.get(i).getPosY().get(xy)].
                                            getSnakeModels().get(0).getPlayerNr()).setScore(snakeModels.get(
                                    Spielfeld[snakeModels.get(i).getPosX().get(xy)]
                                            [snakeModels.get(i).getPosY().get(xy)].
                                            getSnakeModels().get(0).getPlayerNr()).getScore() + 4);



                        }
                    }

                }

            }
                    */

            // collision with snake body
            // && i >= 0 überflüssig
            // feach oder  for (Player p : snakeModels) 
            for (int i = 0; i < snakeModels.size(); i++) {
                snakeModels.get(i).setBestPlayer(false);
                // Besser: 
                // if (!snakeModels.get(i).getPlayerAlife()) {
                //    continue;
                // }
                if (snakeModels.get(i).getPlayerAlife()) {
                    snakeModels.get(i).setPlayedTime(50);

                    if (snakeModels.get(i).getPosXHead() == snakeFodder.getPosX() && snakeModels.get(i).getPosYHead() == snakeFodder.getPosY()) {
                        snakeModels.get(i).setScore((int) (snakeModels.get(i).getScore() * 1.25f) + 3); // neu dazugewonnene Blöcke
                        snakeFodder.setNewPosition();
                        webSocket.convertAndSend("/snake/fodderOfSnake", snakeFodder);
                    }

                    for (int snakeBodyCouter = 0; snakeBodyCouter < snakeModels.size() && !exit; snakeBodyCouter++) {
                        if (snakeBodyCouter != i) {
                            for (int bodyLength = 0; bodyLength < snakeModels.get(snakeBodyCouter).getLengthOfBody(); bodyLength++) {
                                if (snakeModels.get(i).getPosXHead() == snakeModels.get(snakeBodyCouter).getPosX().get(bodyLength) &&
                                        snakeModels.get(i).getPosYHead() == snakeModels.get(snakeBodyCouter).getPosY().get(bodyLength)) {

                                    if (!snakeModels.get(i).reduceScore()) {
                                        //snakeModels.remove(i);
                                        snakeModels.get(i).setPlayerAlife(false);
                                        //anzPlayer--;
                                        webSocket.convertAndSend("/snake/deleted",
                                                "{\"" + "deletedPlayer\": " + i + "}");
                                        exit = true;
                                        break;
                                    }
                                    snakeModels.get(snakeBodyCouter).setScore(snakeModels.get(snakeBodyCouter).getScore() + 5);
                                }
                            }
                        } else {
                            for (int bodyLength = 0; bodyLength < snakeModels.get(snakeBodyCouter).getLengthOfBody() - 1; bodyLength++) {
                                if (snakeModels.get(i).getPosXHead() == snakeModels.get(snakeBodyCouter).getPosX().get(bodyLength) &&
                                        snakeModels.get(i).getPosYHead() == snakeModels.get(snakeBodyCouter).getPosY().get(bodyLength)) {

                                    System.out.println("Someone HITTED");

                                    if (!snakeModels.get(i).reduceScore()) {
                                        //snakeModels.remove(i);
                                        snakeModels.get(i).setPlayerAlife(false);
                                        //anzPlayer--;
                                        webSocket.convertAndSend("/snake/deleted",
                                                "{\"" + "deletedPlayer\": " + i + "}");
                                        exit = true;
                                        break;
                                    }
                                }
                            }

                        }

                    }
                    exit = false;
                }
            }

            // send the bestPlayer to all clients
            webSocket.convertAndSend("/snake/newHighScore", determineBestPlayer());

            if (snakeModels == null) {
                return;
            }

            while (snakeFodder != null && snakeFodder.isPause()) {
                // game pause
            }

            while (System.currentTimeMillis() <= (time + 5 * kästchenGröße)) {
                // Waiting for next Step
            }
        }
    }

    private Player determineBestPlayer(){
        // best set Player
        for (int i = 0; i < snakeModels.size(); i++) {
            if (snakeModels.get(i).getScore() > bestScore) {
                bestScore = snakeModels.get(i).getScore();

                idBestPlayer = i;
            }
        }

        // show the best score associated with player
        for (Player p : players) {
            if (p.getName().equals(snakeModels.get(idBestPlayer).getPlayerName())) {
                bestPlayer = p;
                break;
            }
        }

        snakeModels.get(idBestPlayer).setBestPlayer(true);

        if (bestScoreOfAll < bestScore && bestPlayer.getBestScore() < snakeModels.get(idBestPlayer).getScore()) {
            bestPlayer.setBestScore(snakeModels.get(idBestPlayer).getScore());
            bestScoreOfAll = bestScore;
        }
        return bestPlayer;
    }

    @PostMapping("/changeDirection")
    public void snakeChangeDirection(@RequestParam String changeD) {
        String[] snakeModelData = changeD.split(";");
        // snakeModels.get(Integer.parseInt(snakeModelData[1])).setDirection(snakeModelData[0]);
        if (snakeModelData[0].equals("pause")) {
            snakeFodder.togglePause();
            webSocket.convertAndSend("/snake/fodderOfSnake", snakeFodder);
            return;
        }
        if (snakeDirection(snakeModelData[0], Integer.parseInt(snakeModelData[1]))) {
            // System.out.println("Correct: true");
            long wait = System.currentTimeMillis();
            while (wait + 40 >= System.currentTimeMillis()) ;

            snakeModels.get(Integer.parseInt(snakeModelData[1])).setDirection(snakeModelData[0]);
// TODO direction List
            /// Klasse SnakeModel des Spielers mit der Richtungsänderung wird an alle Clients versendet
            //webSocket.convertAndSend("/snake/changeDofP", snakeModels.get(Integer.parseInt(snakeModelData[1])));
            webSocket.convertAndSend("/snake/changeDofP", snakeModels);

        } else {
            // System.out.println("Correct: false");
        }
    }

    // delete the snake of dead player
    @PostMapping("/playerDead")
    public void snakePlayerDead(@RequestParam int deadPlayerNr) {
        if (snakeModels == null || snakeModels.size() <= deadPlayerNr) {
            return;
        }
        // reduce score until it´s zero
        while (snakeModels.get(deadPlayerNr).getScore() != 0) {
            snakeModels.get(deadPlayerNr).reduceScore();
        }
    }

    // receive messages of clients and catch orders
    @PostMapping("/chat")
    public void chatController(@RequestParam int playerNr, @RequestParam String message) {
        // reset all variables
        if (message.equals("/restart Game")) {
            resetFunction();
            return;
        }

        // raise score of the player which gave the order
        if (message.contains("/get Score ")) {
            String[] getScore = message.split("/get Score ");
            snakeModels.get(playerNr).setScore(snakeModels.get(playerNr).getScore() + Integer.parseInt(getScore[1]));
            return;
        }

        // send the message to all clients
        webSocket.convertAndSend("/snake/chat",
                new ChatMessageDTO(message, playerNr, snakeModels.get(playerNr).getPlayerColor()));
    }

    // check input of direction
    public boolean snakeDirection(String newDirection, int playerNr) {
        // direction logic: same direction as before and opposite direction return false
        if (newDirection.equals(snakeModels.get(playerNr).getDirection())) {
            return false;
        } else if (newDirection.equals("o")) {
            return !snakeModels.get(playerNr).getDirection().equals("u");
        } else if (newDirection.equals("u")) {
            return !snakeModels.get(playerNr).getDirection().equals("o");
        } else if (newDirection.equals("r")) {
            return !snakeModels.get(playerNr).getDirection().equals("l");
        } else if (newDirection.equals("l")) {
            return !snakeModels.get(playerNr).getDirection().equals("r");
        }

        System.out.println("Fatal Error in snakeDirection");
        return false;
    }

    public void setFeldArray() {
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 60; y++) {
                Spielfeld[x][y] = new FieldData();
            }
        }
    }

    public void setSnakePositionOnField(int indexOfSnake) {
        int indexSize;
        if (snakeModels.get(indexOfSnake).getPosX().size() != snakeModels.get(indexOfSnake).getPosY().size()) {
            return;
        }
        indexSize = snakeModels.get(indexOfSnake).getPosX().size() - 1;

        int posX, posY;
        for (int i = 0; i < indexSize; i++) {
            posX = snakeModels.get(indexOfSnake).getPosX().get(i);
            posY = snakeModels.get(indexOfSnake).getPosY().get(i);
            if (posX < 100 && posY < 60) {
                Spielfeld[posX / 10][posY / 10].getSnakeModels().add(snakeModels.get(indexOfSnake));
            }
        }
    }

    public void setFieldEmpty() {
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 60; y++) {
                Spielfeld[x][y].getSnakeModels().clear();
            }
        }
    }

    public boolean resetFunction() {
        snakeModels.clear();
        snakeModels = null;
        snakeFodder = null;
        playerAlife = null;
        anzPlayer = 0;
        isRunning = false;
        players = null;
        bestScoreOfAll = 0;

        return true;
    }
}