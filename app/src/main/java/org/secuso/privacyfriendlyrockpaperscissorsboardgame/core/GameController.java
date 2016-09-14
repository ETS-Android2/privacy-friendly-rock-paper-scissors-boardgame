package org.secuso.privacyfriendlyrockpaperscissorsboardgame.core;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import org.secuso.privacyfriendlyrockpaperscissorsboardgame.R;
import org.secuso.privacyfriendlyrockpaperscissorsboardgame.activities.GameActivity;
import org.secuso.privacyfriendlyrockpaperscissorsboardgame.activities.HomeActivity;
import org.secuso.privacyfriendlyrockpaperscissorsboardgame.ui.FightDialog;
import org.secuso.privacyfriendlyrockpaperscissorsboardgame.ui.RPSBoardLayout;
import org.secuso.privacyfriendlyrockpaperscissorsboardgame.ui.RPSFieldView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by david on 06.05.2016.
 */
public class GameController implements Animation.AnimationListener {
    private static Toast toast;
    public static final int MODE_NORMAL_AUTO = 0;
    public static final int MODE_ROCKPAPERSCISSORSLIZARDSPOCK_AUTO = 1;
    public static final int MODE_NORMAL_MANUAL = 2;
    public static final int MODE_ROCKPAPERSCISSORSLIZARDSPOCK_MANUAL = 3;
    int gameMode;
    boolean ai;
    IPlayer p0;
    IPlayer p1;
    IPlayer playerOnTurn;
    int fieldX;
    int fieldY;
    RPSBoardLayout view;
    GameState model;
    boolean cellSelected;
    int selX;
    int selY;
    Context context;
    boolean gameFinished;
    Move drawMove;
    RPSFigure attackerFigure;
    RPSFigure attackedFigure;
    List<RPSGameFigure> startingTeamP0;
    List<RPSGameFigure> startingTeamP1;

    /**
     * Starts a new Game in Standard 8x8 Layout
     */
    public GameController(Context context, GameState model) {
        this.fieldX = 8;
        this.fieldY = 8;
        this.model = model;
        cellSelected = false;
        this.context = context;
        this.gameFinished = this.model.isGameFinished();
        this.gameMode = model.getGameMode();
        this.ai = model.isAgainstAI();
        this.toast = new Toast(this.context);
    }

    public int getX() {
        return fieldX;
    }

    public int getY() {
        return fieldY;
    }

    /**
     * Starts a new game. Creates the model, requests the starting assignments from both players and initializes the first turn
     *
     * @param view the Board view to use as view
     */
    public void startGame(RPSBoardLayout view) {
        if (this.model.gameStateIsReady()) {
            IPlayer[] players = this.model.getPlayers();
            this.p0 = players[0];
            this.p1 = players[1];
            this.playerOnTurn = this.model.getPlayerOnTurn();
        } else {
            switch (this.gameMode) {
                case MODE_NORMAL_AUTO:
                    this.p0 = this.ai ? new AIPlayer(0, -1) : new NormalPlayer(0, -1);
                    this.p1 = new NormalPlayer(1, ContextCompat.getColor(this.context, R.color.colorAccent));
                    break;
                case MODE_ROCKPAPERSCISSORSLIZARDSPOCK_AUTO:
                    this.p0 = this.ai ? new LizardSpockAIPlayer(0, -1) : new LizardSpockPlayer(0, -1);
                    this.p1 = new LizardSpockPlayer(1, ContextCompat.getColor(this.context, R.color.colorAccent));
                    break;
                case MODE_NORMAL_MANUAL:
                    this.p0 = this.ai ? new AIPlayer(0, -1) : new NormalPlayer(0, -1);
                    this.p1 = new NormalPlayer(1, ContextCompat.getColor(this.context, R.color.colorAccent));
                    break;
                case MODE_ROCKPAPERSCISSORSLIZARDSPOCK_MANUAL:
                    this.p0 = this.ai ? new LizardSpockAIPlayer(0, -1) : new LizardSpockPlayer(0, -1);
                    this.p1 = new LizardSpockPlayer(1, ContextCompat.getColor(this.context, R.color.colorAccent));
                    break;

            }
            this.model.setPlayer(this.p0, this.p1);
            this.playerOnTurn = this.p1;
            this.model.setPlayerOnTurn(this.playerOnTurn);
            if(gameMode==this.MODE_ROCKPAPERSCISSORSLIZARDSPOCK_MANUAL||gameMode==this.MODE_NORMAL_MANUAL){
                if(p0.isAi()){
                    startingTeamP0=p0.provideInitialAssignment(16);
                }
                else view.showAssignmentDialog(p0,this.gameMode);
                if(p1.isAi()){
                    startingTeamP1=p1.provideInitialAssignment(16);
                }
                else view.showAssignmentDialog(p1,gameMode);
            }
            else{
                startingTeamP0=p0.provideInitialAssignment(16);
                startingTeamP1=p1.provideInitialAssignment(16);
                this.model.setGamePane(this.placeFigures(startingTeamP0,startingTeamP1));
                this.model.setGameStateOK();
            }
        }
        this.view = view;
        if(!model.gameStateIsReady())
            return;
        this.view.drawFigures(this.getRepresentationForPlayer(), this.playerOnTurn);
    }

    /**
     * Places the figures of both players randomly in the correct spots for the starting assignment
     *
     * @param figuresP0 the figures of p0
     * @param figuresP1 the figures of p1
     * @return the starting gamepane
     */
    public RPSGameFigure[][] placeFigures(List<RPSGameFigure> figuresP0, List<RPSGameFigure> figuresP1) {
        RPSGameFigure[][] gamePane = new RPSGameFigure[fieldY][fieldX];
        for (int i = 0; i < gamePane.length; i++) {
            for (int j = 0; j < gamePane[i].length; j++) {
                gamePane[i][j] = null;
            }
        }
        for (int i = 0; i < 16; i++) {
            gamePane[i / 8][i % 8] = figuresP0.get(i);
        }
        for (int i = 0; i < 16; i++) {
            gamePane[6 + i / 8][i % 8] = figuresP1.get(i);
        }
        return gamePane;

    }

    /**
     * Implements the Figure Hiding Game and the bottom up view for the player that is currently on turn.
     *
     * @return
     */
    public RPSGameFigure[][] getRepresentationForPlayer() {
        RPSGameFigure[][] originalGamePane = model.getGamePane();
        RPSGameFigure[][] representationForPlayer = new RPSGameFigure[originalGamePane.length][originalGamePane[0].length];
        if (playerOnTurn.equals(p0)) {
            for (int i = 0; i < originalGamePane.length; i++) {
                for (int j = 0; j < originalGamePane[i].length; j++) {
                    representationForPlayer[getY() - 1 - i][getX() - 1 - j] = originalGamePane[i][j];
                }
            }
        } else return originalGamePane;
        return representationForPlayer;
    }

    public void forceRedraw() {
        if(drawMove==null)
        this.view.drawFigures(this.getRepresentationForPlayer(), playerOnTurn);
    }

    public void playerMove(int xTarget, int yTarget) {
        boolean deselect = true;
        try {
            deselect = validateMove(new Move(this.selX, this.selY, xTarget, yTarget));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (deselect) {
            this.deselect();
            this.forceRedraw();
        }
    }

    public void selectCell(int x, int y) {
        //Toast.makeText(this.context, "Select Cell "+y+", "+x, Toast.LENGTH_SHORT).show();
        this.cellSelected = true;
        this.selX = x;
        this.selY = y;
        RPSGameFigure[][] board = this.getRepresentationForPlayer();
        if (board[y][x] != null) {
            if (!board[y][x].getOwner().equals(this.playerOnTurn)) {
                this.cellSelected = false;
                this.toast.cancel();
                this.toast = Toast.makeText(this.context, R.string.sToastEnemy, Toast.LENGTH_SHORT);
                this.toast.show();
                return;
            }
            if (board[y][x].getType() != RPSFigure.FLAG)
                this.view.highlightDestinations(this.getValidDestinations(new Coordinate(x, y), playerOnTurn),new Coordinate(x, y));
            else {
                this.cellSelected = false;
                this.toast.cancel();
                this.toast = Toast.makeText(this.context, R.string.sToastFlag, Toast.LENGTH_SHORT);
                this.toast.show();
                return;
            }
        } else {
            this.toast.cancel();
            this.toast = Toast.makeText(this.context, R.string.sToastEmpty, Toast.LENGTH_SHORT);
            this.toast.show();
        }
    }

    public void deselect() {
            forceRedraw();
        this.cellSelected = false;
    }

    public boolean isCellSelected() {
        return this.cellSelected;
    }

    public int getSelX() {
        return this.selX;
    }

    public int getSelY() {
        return this.selY;
    }

    private boolean validateMove(final Move move) throws InterruptedException {
        //Check if movement direction and length is fine
        if (Math.abs(move.getxStart() - move.getxTarget()) == 1) {
            if (Math.abs(move.getyStart() - move.getyTarget()) == 0) {
                ;
            } else {
                //Toast.makeText(this.context, "Invalid Move", Toast.LENGTH_SHORT).show();
                this.deselect();
                this.forceRedraw();
                this.selectCell(move.getxTarget(), move.getyTarget());
                return false;
            }
        } else if (Math.abs(move.getxStart() - move.getxTarget()) == 0) {
            if (Math.abs(move.getyStart() - move.getyTarget()) == 1) {
                ;
            } else {
                // Toast.makeText(this.context, "Invalid Move", Toast.LENGTH_SHORT).show();
                this.deselect();
                this.forceRedraw();
                this.selectCell(move.getxTarget(), move.getyTarget());
                return false;
            }
        } else {
            //Toast.makeText(this.context, "Invalid Move", Toast.LENGTH_SHORT).show();
            this.deselect();
            this.forceRedraw();
            this.selectCell(move.getxTarget(), move.getyTarget());
            return false;
        }
        //Check if source field is occupied
        RPSGameFigure[][] gamePane = this.model.getGamePane();
        if (playerOnTurn.getId() == p1.getId()) {
            if (gamePane[move.getyStart()][move.getxStart()] == null) {
                return true;
            }
        } else if (gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()] == null) {
            return true;
        }
        //Check if figure on field is of current Player
        if (playerOnTurn.getId() == p1.getId()) {
            if (gamePane[move.getyStart()][move.getxStart()].getOwner().getId() != p1.getId()) {
                return true;
            }
        } else if (gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getOwner().getId() != p0.getId()) {
            return true;
        }
        if (playerOnTurn.getId() == p1.getId()) {
            if (gamePane[move.getyStart()][move.getxStart()].getType() == RPSFigure.FLAG) {
                this.toast.cancel();
                this.toast = Toast.makeText(this.context, "Flags can not move", Toast.LENGTH_SHORT);
                this.toast.show();
                return true;
            }
            //Check if target field is empty
            if (gamePane[move.getyTarget()][move.getxTarget()] == null) {
                gamePane[move.getyTarget()][move.getxTarget()] = gamePane[move.getyStart()][move.getxStart()];
                gamePane[move.getyStart()][move.getxStart()] = null;
                this.model.setGamePane(gamePane);
                this.view.animateTurn(move, this);
                //nextTurn();
                return true;
            } else {
                if (gamePane[move.getyTarget()][move.getxTarget()].getOwner().getId() == p0.getId()) {
                    if (gamePane[move.getyStart()][move.getxStart()].getType() == gamePane[move.getyTarget()][move.getxTarget()].getType()) {
                        if (gamePane[move.getyStart()][move.getxStart()].getOwner().isAi())
                            this.attackerFigure = gamePane[move.getyStart()][move.getxStart()].getOwner().getNewType();
                        else
                            view.getNewType(this.gameMode, gamePane[move.getyStart()][move.getxStart()].getOwner(), true);
                        if (gamePane[move.getyTarget()][move.getxTarget()].getOwner().isAi())
                            this.attackerFigure = gamePane[move.getyTarget()][move.getxTarget()].getOwner().getNewType();
                        else
                            view.getNewType(this.gameMode, gamePane[move.getyTarget()][move.getxTarget()].getOwner(), false);
                        this.drawMove = move;
                        return true;
                    }
                    this.deselect();
                    this.forceRedraw();
                    gamePane[move.getyTarget()][move.getxTarget()] = attack(gamePane[move.getyStart()][move.getxStart()], gamePane[move.getyTarget()][move.getxTarget()], move, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            GameController.this.view.animateFight(move, GameController.this);
                        }
                    });
                    if (!this.gameFinished) {
                        gamePane[move.getyTarget()][move.getxTarget()].discover();
                        gamePane[move.getyStart()][move.getxStart()] = null;
                        this.model.setGamePane(gamePane);
                        //this.view.animateFight(move,this);
                        //nextTurn();
                    }
                    return false;
                } else {
                    this.deselect();
                    this.forceRedraw();
                    this.selectCell(move.getxTarget(), move.getyTarget());
                    return false;
                }
            }
        } else {
            if (gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getType() == RPSFigure.FLAG) {
                this.toast.cancel();
                this.toast = Toast.makeText(this.context, "Flags can not move", Toast.LENGTH_SHORT);
                this.toast.show();
                return true;
            }
            //Check if target field is empty
            if (gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()] == null) {
                gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()] = gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()];
                gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()] = null;
                this.model.setGamePane(gamePane);
                this.view.animateTurn(move, this);
                return true;
            } else {
                if (gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()].getOwner().getId() == p1.getId()) {
                    if (gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getType() == gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxStart()].getType()) {
                        this.drawMove = move;
                        if (gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getOwner().isAi())
                            this.attackedFigure = gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getOwner().getNewType();
                        else
                            this.view.getNewType(this.gameMode, gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()].getOwner(), true);
                        if (gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()].getOwner().isAi())
                            this.attackedFigure = gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()].getOwner().getNewType();
                        else
                            this.view.getNewType(this.gameMode, gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()].getOwner(), false);
                        return true;
                    }
                    this.deselect();
                    this.forceRedraw();
                    gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()] = attack(gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()], gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxStart()], move, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            GameController.this.view.animateFight(move, GameController.this);
                        }
                    });
                    if (!this.gameFinished) {
                        gamePane[getY() - 1 - move.getyTarget()][getX() - 1 - move.getxTarget()].discover();
                        gamePane[getY() - 1 - move.getyStart()][getX() - 1 - move.getxStart()] = null;
                        this.model.setGamePane(gamePane);
                        //this.view.animateFight(move,this);
                    }
                    return false;
                } else {
                    this.deselect();
                    this.forceRedraw();
                    this.selectCell(move.getxTarget(), move.getyTarget());
                    return false;
                }
            }
        }
    }

    void updateModelAndView(RPSGameFigure[][] newPane) {
        this.view.drawFigures(this.getRepresentationForPlayer(), this.playerOnTurn);
        nextTurn();
    }

    void nextTurn() {
        if (playerOnTurn.getId() == p1.getId())
            playerOnTurn = p0;
        else playerOnTurn = p1;
        this.model.setPlayerOnTurn(playerOnTurn);
        view.handleTurnover(playerOnTurn);
    }

    RPSGameFigure attack(RPSGameFigure attacker, RPSGameFigure attacked, Move move, DialogInterface.OnDismissListener listener) {
        if (attacked.getType() == RPSFigure.FLAG) {
            handleGameFinished();
            move.setWon();
            return attacker;
        }
        if (attacked.getType().getsBeatenBy(attacker.getType())) {
            view.showFightDialog(attacker, attacked, attacker, listener);
            move.setWon();
            return attacker;
        } else {
            view.showFightDialog(attacker, attacked, attacked, listener);
            return attacked;
        }
    }

    private void handleGameFinished() {
        this.model.gameIsFinished();
        this.gameFinished = true;
        for (RPSGameFigure[] row : this.getRepresentationForPlayer()) {
            for (RPSGameFigure fig : row) {
                if (fig != null)
                    fig.discover();
            }
        }
        this.forceRedraw();
        view.showWinDialog();
    }

    private List<Coordinate> getValidDestinations(Coordinate origin, IPlayer player) {
        ArrayList<Coordinate> result = new ArrayList<>();
        ArrayList<Coordinate> temp = new ArrayList<>();
        RPSGameFigure[][] fieldFromPlayer = this.getRepresentationForPlayer();
        Coordinate c1 = new Coordinate(origin.getX() - 1, origin.getY());
        Coordinate c2 = new Coordinate(origin.getX() + 1, origin.getY());
        Coordinate c3 = new Coordinate(origin.getX(), origin.getY() - 1);
        Coordinate c4 = new Coordinate(origin.getX(), origin.getY() + 1);
        temp.add(c1);
        temp.add(c2);
        temp.add(c3);
        temp.add(c4);
        for (Coordinate c : temp) {
            if (isValid(c, fieldFromPlayer, player))
                result.add(c);
        }
        return result;
    }

    private boolean isValid(Coordinate c, RPSGameFigure[][] field, IPlayer player) {
        if (c.getX() >= this.getX() || c.getY() >= this.getY() || c.getX() < 0 || c.getY() < 0)
            return false;
        if (field[c.getY()][c.getX()] == null)
            return true;
        else if (field[c.getY()][c.getX()].getOwner().equals(player))
            return false;
        else return true;
    }

    public boolean isGameFinished() {
        return this.gameFinished;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        this.deselect();
        updateModelAndView(this.model.getGamePane());
    }

    public void handleDrawRoutine(int selection, boolean attacker) {
        if (drawMove == null)
            throw new RuntimeException("handleDrawRoutine was called but there was no draw to handle");
        RPSFigure newType;
        switch (selection) {
            case 0:
                newType = RPSFigure.ROCK;
                break;
            case 1:
                newType = RPSFigure.PAPER;
                break;
            case 2:
                newType = RPSFigure.SCISSOR;
                break;
            case 3:
                newType = RPSFigure.LIZARD;
                break;
            default:
                newType = RPSFigure.SPOCK;
                break;
        }
        if (attacker) {
            this.attackerFigure = newType;
        } else this.attackedFigure = newType;
        if (attackerFigure != null && attackedFigure != null) {
            RPSGameFigure[][] gamePane = this.model.getGamePane();
            if (playerOnTurn.equals(p1)) {
                gamePane[drawMove.getyStart()][drawMove.getxStart()] = new RPSGameFigure(p1, attackerFigure);
                gamePane[drawMove.getyTarget()][drawMove.getxTarget()] = new RPSGameFigure(p0, attackedFigure);
            } else {
                gamePane[getY() - 1 - drawMove.getyStart()][getX() - 1 - drawMove.getxStart()] = new RPSGameFigure(p0, attackerFigure);
                gamePane[getY() - 1 - drawMove.getyTarget()][getX() - 1 - drawMove.getxTarget()] = new RPSGameFigure(p1, attackedFigure);
            }
            this.model.setGamePane(gamePane);
            this.forceRedraw();
            this.selectCell(drawMove.getxStart(), drawMove.getyStart());
            int xTarget = drawMove.getxTarget();
            int yTarget = drawMove.getyTarget();
            this.drawMove = null;
            this.attackerFigure = null;
            this.attackedFigure = null;
            this.playerMove(xTarget, yTarget);
            //Set up for next draw
        }

    }

    public void submitStartingTeam(Map<RPSFigure,Integer> team, IPlayer player){
        List<RPSGameFigure> lineup=new ArrayList<>();
        for(Map.Entry<RPSFigure,Integer> entry: team.entrySet()){
            for(int i=0;i<entry.getValue();i++){
                lineup.add(new RPSGameFigure(player,entry.getKey()));
            }
        }
        lineup.add(new RPSGameFigure(player,RPSFigure.FLAG));
        Random rand = new Random();
        Calendar cal = Calendar.getInstance();
        rand.setSeed(cal.getTimeInMillis());
        Collections.shuffle(lineup,rand);
        if(player.equals(p0))
            startingTeamP0=lineup;
        else startingTeamP1=lineup;
        if(startingTeamP0!=null&&startingTeamP1!=null){
            this.model.setGamePane(this.placeFigures(startingTeamP0,startingTeamP1));
            this.model.setGameStateOK();
            this.view.drawFigures(getRepresentationForPlayer(),playerOnTurn);
        }
    }


    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}
