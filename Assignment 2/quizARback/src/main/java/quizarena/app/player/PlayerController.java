package quizarena.app.player;

// this is our REST controller, our RESTapi

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "player")
public class PlayerController {

    private final PlayerRepository playerRepository;

    // this is a dependency injection
    @Autowired
    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // this is a Post request -> it connects our backend to the database
    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        // we're gonna use the data in the Player object -> to create a new document in the database
        // Put information from frontend into a new Player object
        Player newPlayer = new Player(player.getId(), player.getFirebaseUID(), player.getNickname(), player.getFriendsFirebaseUIDs());

        // Auto-convert newPLayer to JSON and insert into new database
        playerRepository.insert(newPlayer);

        // Return the new player that we created in the database
        return newPlayer;
    }

    @GetMapping(path = "{firebaseUID}")
    public Player findPlayerByFirebaseUID(@PathVariable String firebaseUID) {
        // what PathVariable does, is: once we go to localhost:8080/player we can add another slash and add the firebaseUID after it -> hit enter -> then we get Player back as JSON object
        return playerRepository.findPlayerByFirebaseUID(firebaseUID)
                .orElseThrow(() -> new IllegalStateException("Error in findPlayerByFirebaseUID: No player with this firebaseUID: " + firebaseUID + " in MongoDB database"));
    }

    @GetMapping(path = "/nickname/{firebaseUID}")
    public String findPlayerNicknameByFirebaseUID(@PathVariable String firebaseUID) {
        // what PathVariable does, is: once we go to localhost:8080/player we can add another slash and add the firebaseUID after it -> hit enter -> then we get Player back as JSON object
        Player currentPlayer = playerRepository.findPlayerByFirebaseUID(firebaseUID)
                .orElseThrow(() -> new IllegalStateException("Error in findPlayerByFirebaseUID: No player with this firebaseUID: " + firebaseUID + " in MongoDB database"));
        return currentPlayer.getNickname();
    }

}
