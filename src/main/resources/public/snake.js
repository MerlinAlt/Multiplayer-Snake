// 192.168.0.x87 +:8080/snake.html
var canvas, ctx, playerNr = -1, maxPlayer = 0;
var fodderX = 100, fodderY = 100;
var pause = false, check;

window.onload = function () {
    check = prompt('Gib deinen Namen ein', '');
    if (check === '') {
        check = "Inkompetente Person";
    }
    initialization();
};

window.onbeforeunload = function () {
    if (playerNr === -1 || maxPlayer === 0) {
        return;
    }
    fetch("http://" + "10.62.2.194" + ":8080/api/snake/playerDead?deadPlayerNr=" + playerNr, {
        method: 'POST',
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}
    }).then(function (ev) {


    }).catch((function (error) {
        console.log("Error: ", error);
    }));
};

function initialization() {
    document.getElementById('messages').innerHTML = "";

    canvas = document.getElementById("canvas");
    if (canvas.getContext) {
        ctx = canvas.getContext('2d');
        connectWebSocketChangeDirection(() => {
            fetch("http://" + "10.62.2.194" + ":8080/api/snake/newPlayer?playerName=" + check, {
                method: 'POST',
                headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}
            }).then(function (ev) {
                if (ev.status !== 200) {
                    console.log("Error: not 200");
                    return;
                }
                console.log("Erzeugte Snake Klasse ohne JSON:\n\n" + ev.toString());
                ev.json().then(function (snakeClass) {
                    console.log("Erzeugte Snake Klasse:\n\n" + snakeClass.playerNr);
                    playerNr = snakeClass.playerNr;
                });
                startGame();
            }).catch((function (error) {
                console.log("Error: ", error);
            }));

        });

        // Connection Backend & PlayerGetID from Backend
    } else {
        alert("I am sorry, but your browser is bullshit. It does not support the canvas tag.");

    }

}

function startGame() {
    fetch("http://" + "10.62.2.194" + ":8080/api/snake/runGame", {
        method: 'GET',
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}
    }).then(function (ev) {


    }).catch((function (error) {
        console.log("Error: ", error);
    }));
}

function drawSnakes(color, posX, posY, partOfHead) {
    ctx.beginPath();
    //ctx.fillStyle = "rgb(250,0,0)";

    ctx.fillStyle = color;
    if (partOfHead === 0) {
        ctx.strokeStyle = "#000000";
    } else if (partOfHead === 404) {
        ctx.strokeStyle = "#ff0000";
    } else {
        ctx.strokeStyle = "#ffffff";
    }


    var width = 10;
    var height = 10;

    ctx.lineWidth = 1.5;
    ctx.fillRect(posX, posY, width, height);

    ctx.strokeRect(posX, posY, width, height);
    //ctx.clearRect(x,y,width,height);
    ctx.closePath();
}

var interval = setInterval(getPosition, 5);

function getPosition() {
    //ctx.clearRect(0, 0, canvas.width, canvas.height);

    /*

    drawSnakes("blue", i, 100);

    if (i === 999) {
        i = 0;
    }
    i++;

     */
    document.onkeydown = function (event) {
        var keyCode, changeD = "Error";
        var sendKeyCode = false;

        if (event == null) {
            keyCode = window.event.keyCode;
        } else {
            keyCode = event.keyCode;
        }


        //alert("Eingabe: " + keyCode);
        switch (keyCode) {
            // left
            case 65:
            case 37:
                changeD = "l";
                sendKeyCode = true;
                // action when pressing left key
                break;
            // up
            case 87:
            case 38:
                changeD = "o";
                sendKeyCode = true;
                // action when pressing up key
                break;

            // right
            case 68:
            case 39:
                changeD = "r";
                sendKeyCode = true;
                // action when pressing right key
                break;
            // down
            case 83:
            case 40:
                changeD = "u";
                sendKeyCode = true;
                // action when pressing down key
                break;
            case 8:
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                // action when pressing down key
                break;
            case 32:
                changeD = "pause";
                sendKeyCode = true;
                break;
            default:
                console.log("Fehlerhafte Eingabe Code: " + keyCode);
                break;
        }

        if (sendKeyCode && changeD !== "Error") {
            changeD += ";" + playerNr.toString();
            //changeD = changeD.toString();
            fetch("http://" + "10.62.2.194" + ":8080/api/snake/changeDirection?changeD=" + changeD, {
                method: 'POST',
                headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}
            }).then(function (ev) {
                if (ev.status !== 200) {
                    console.log("Errorcode: " + ev.status);
                    console.log("ChangeD ist gleich: " + changeD);
                }

            }).catch((function (error) {
                console.log("Error: ", error);
            }));

            changeD = "Error";
        }
    };
}


//setTimeout(getPosition, 5);

function connectWebSocketChangeDirection(succesFunction) {
    let socket = new WebSocket("ws://" + "10.62.2.194" + ":8080/ws");
    let ws = Stomp.over(socket);
    let that = this;
    ws.connect({}, (frame) => {
        ws.subscribe("/snake/fodderOfSnake", (message) => {
            let newFodder = JSON.parse(message.body);
            fodderX = newFodder.posX * 10;
            fodderY = newFodder.posY * 10;
            console.log("NewFodder: ", message)
        });
        ws.subscribe("/snake/deleted", (message) => {
            let newFodder = JSON.parse(message.body);
            if (newFodder.deletedPlayer != null && newFodder.deletedPlayer === playerNr) {
                alert("Du bist offensichtlich weniger talentiert als dein Gegner.\nDu bist tot!\nSeite neuladen um wiedereinzusteigen ;-)");
                alert("Bei \"Ok\" wird die Seite neugeladen");
                location.reload(true);
                socket.close();
            }
            console.log("NewFodder: ", message);
        });
        ws.subscribe("/snake/changeDofP", (message) => {
            // console.log("WebSocket is Connected\nVariable Message: ", message);
            let snakeNewData = JSON.parse(message.body);

            maxPlayer = snakeNewData.length;
            console.log("MaxPlayer: " + maxPlayer);
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            drawSnakes("#FF0000", fodderX, fodderY, 404);
            document.getElementById('spanId').innerHTML = "";

            for (var currentPlayer = 0; currentPlayer < maxPlayer; currentPlayer++) {

                console.log("Player: " + currentPlayer + " has a Score of:" +
                    snakeNewData[currentPlayer].score);

                if (snakeNewData[currentPlayer].score !== null && snakeNewData[currentPlayer].score !== 0) {

                    if (snakeNewData[currentPlayer].bestPlayer) {
                        document.getElementById('spanId').innerHTML += "<h3><font color=\"" +
                            snakeNewData[currentPlayer].playerColor + "\">" + "Player: " +
                            currentPlayer + " " + snakeNewData[currentPlayer].playerName +
                            " </font>has a Score of: " +
                            snakeNewData[currentPlayer].score + "   and died " +
                            snakeNewData[currentPlayer].playerDeaths +
                            " times</h3>";
                    } else {
                        document.getElementById('spanId').innerHTML += "<font color=\"" +
                            snakeNewData[currentPlayer].playerColor + "\">" + "Player: " +
                            currentPlayer + " " + snakeNewData[currentPlayer].playerName +
                            " </font>has a Score of: " +
                            snakeNewData[currentPlayer].score + "   and died " +
                            snakeNewData[currentPlayer].playerDeaths +
                            " times";
                    }
                    document.getElementById('spanId').innerHTML += "<br>";
                }
                if (snakeNewData[currentPlayer].posX === null || snakeNewData[currentPlayer].posY === null
                    && snakeNewData[currentPlayer].posX.length === 0) {

                } else if (snakeNewData[currentPlayer].posX.length === snakeNewData[currentPlayer].posY.length) {
                    for (var i = 0; i < snakeNewData[currentPlayer].posX.length; i++) {
                        drawSnakes(snakeNewData[currentPlayer].playerColor,
                            snakeNewData[currentPlayer].posX[i] * 10, snakeNewData[currentPlayer].posY[i] * 10,
                            snakeNewData[currentPlayer].posX.length - (i + 1));
                    }
                }
            }
        });
        ws.subscribe("/snake/chat", (message) => {
            let theNewMessage = JSON.parse(message.body);

            console.log(theNewMessage.playerNr + " Message: " + theNewMessage.newMessage);

            document.getElementById('messages').innerHTML +=
                "<span style='color:" + theNewMessage.playerColor + ";'> " +
                "Spielernummer " + theNewMessage.playerNr +
                "</span>" + " schrieb folgendes: ";

            document.getElementById('messages').innerHTML += "<span style='color: #707070;'>" +
                theNewMessage.newMessage + "</span><br>";
        });
        ws.subscribe("/snake/newHighScore", (message) => {
            let theNewMessage = JSON.parse(message.body);

            document.getElementById('highScore').innerHTML =
                theNewMessage.name + " reached " +
                theNewMessage.bestScore + " points";

        });
        that.webSocket = ws;
        succesFunction();
    }, function (error) {
        console.log("STOMP error " + error);
        //that.webSocket = null;
    });

}

// cheat/ chat box
function newMessage() {
    let newMessage = document.querySelector("#chatWindow").value;

    fetch("http://" + "10.62.2.194" + ":8080/api/snake/chat?playerNr=" + playerNr + "&message=" + newMessage, {
        method: 'POST',
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}
    }).then(function (ev) {
        console.log("NEW MESSAGE AVAIBLE");
    }).catch((function (error) {
        console.log("Error: ", error);
    }));
}