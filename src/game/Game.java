package game;

import card.Card;
import card.Element;
import card.Type;
import overseer.Overseer;
import user.Player;
import util.Constants;

import java.util.ArrayList;
import java.util.List;

public class Game {
    int id;
    List<Player> players;
    int round = 0;
    boolean isActive = true;

    public Game(List<Player> players) {
        this.id = Constants.RANDOM.nextInt(Constants.RANDOM_ID_FROM, Constants.RANDOM_ID_TO);
        this.players = players;
    }

    public int getID() { return id; }
    public List<Player> getPlayers() { return players; }
    public int getRound() { return round; }
    public boolean getIsActive() { return isActive; }

    private boolean addRound() {
        if (round+1 >= Constants.MAX_ROUNDS_PER_GAME) { return false; }
        round++;
        return true;
    }

    public void makeRound() {
        int loser = checkLoser();
        if (loser != -1) {
            System.out.println(String.format("Winner: %s - Rounds: %d", Overseer.getUser(players.get((loser+1)%2).getID()).getUserName(), round ));
            isActive = false;
            finalizeGame(loser);
            return;
        }
        if (!addRound()) {
            System.out.println("Limit of rounds reached! \nDraw.");
            System.out.println(String.format(
                    "Player 0 had %d cards. Player 1 had %d cards",
                    players.get(0).getDeck().getCards().size(),
                    players.get(1).getDeck().getCards().size())
            );
            isActive = false;
            finalizeGame(-1);
            return;
        }
        System.out.println(String.format("Round %d", round));
        roundPhase1();
    }

    private void roundPhase1() {
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            drawnCards.add(players.get(i).getDeck().drawCard());
        }

        System.out.println(String.format("%s -VS- %s", drawnCards.get(0).getCardName(), drawnCards.get(1).getCardName()));

        int specialEventResult = checkSpecialEvents(drawnCards);
        if (specialEventResult == -1) roundPhase2(drawnCards);
        else endRound(specialEventResult, drawnCards);
    }

    private int checkSpecialEvents(List<Card> drawnCards) {
        Type typeA = drawnCards.get(0).getType();
        Type typeB = drawnCards.get(1).getType();
        if (typeA == Type.GOBLIN && typeB == Type.DRAGON) return 1;
        if (typeA == Type.DRAGON && typeB == Type.GOBLIN) return 0;
        if (typeA == Type.WIZARD && typeB == Type.ORK) return 0;
        if (typeA == Type.ORK && typeB == Type.WIZARD) return 1;
        if (typeA == Type.KNIGHT && (typeB == Type.SPELL && drawnCards.get(1).getElement() == Element.WATER)) return 1;
        if ((typeA == Type.SPELL && drawnCards.get(0).getElement() == Element.WATER) && typeB == Type.KNIGHT) return 0;
        if (typeA == Type.KRAKEN && typeB == Type.SPELL) return 0;
        if (typeA == Type.SPELL && typeB == Type.KRAKEN) return 1;
        if (typeA == Type.DRAGON && (typeB == Type.ELV && drawnCards.get(1).getElement() == Element.FIRE)) return 1;
        if ((typeA == Type.ELV && drawnCards.get(0).getElement() == Element.FIRE) && typeB == Type.DRAGON) return 0;
        return -1;
    }

    private void roundPhase2(List<Card> drawnCards) {

        System.out.println(String.format("> %d  --  %d  Normal Damage", drawnCards.get(0).getDamage(), drawnCards.get(1).getDamage()));

        int damageA = calculateElementalDamage(drawnCards.get(0), drawnCards.get(1));
        int damageB = calculateElementalDamage(drawnCards.get(1), drawnCards.get(0));

        System.out.println(String.format("> %d  --  %d  Elemental Damage", damageA, damageB));

        switch (Integer.signum(damageA - damageB)) {
            case 1: endRound(0, drawnCards); break;
            case 0: endRound(-1, drawnCards); break;
            case -1: endRound(1, drawnCards); break;
        }
    }

    private void endRound(int winnerIndex, List<Card> drawnCards) {
        if (winnerIndex == -1) {
            System.out.println(String.format("Draw. %s + %s", drawnCards.get(0).getCardName(), drawnCards.get(1).getCardName()));
            players.get(0).getDeck().addCard(drawnCards.get(0));
            players.get(1).getDeck().addCard(drawnCards.get(1));
        } else {
            players.get(winnerIndex).getDeck().addCard(drawnCards);
            System.out.println(String.format("Player %d won the round.", winnerIndex));
        }
        System.out.println("--------");
    }

    private int checkLoser() {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getDeck().getCards().size() <= 0) {
                return i;
            }
        }
        return -1;
    }

    private int calculateElementalDamage(Card cardA, Card cardB) {
        float multiplicator = Constants.ELEMENT_MATRIX[castEleToInt(cardA.getElement())][castEleToInt(cardB.getElement())];
        return (int)(cardA.getDamage() * multiplicator);
    }

    private void finalizeGame(int loserIndex) {
        int eloChange = loserIndex == -1 ? 0 : Constants.EloChange(players.get((loserIndex+1)%2).getElo(), players.get(loserIndex).getElo());

        switch (loserIndex) {
            case -1: players.get(0).endGame(eloChange); players.get(1).endGame(eloChange); break;
            case 0: players.get(0).endGame(-eloChange); players.get(1).endGame(eloChange, true); break;
            case 1: players.get(0).endGame(eloChange, true); players.get(1).endGame(-eloChange); break;
        }

        System.out.println(String.format("%s rating: %d\n%s rating: %d", players.get(0).getUserName(), players.get(0).getElo(), players.get(1).getUserName(), players.get(1).getElo()));
        System.out.println("------------------------");
    }

    private int castEleToInt(Element element) {
        switch (element) {
            case FIRE: return 0;
            case WATER: return 1;
            case ICE: return 2;
            case WIND: return 3;
            default: return 4;
        }
    }
}
