package quizarena.app.player;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
    // could return null, if it doesnt find the player with firebaseUID
    Optional<Player> findPlayerByFirebaseUID(String firebaseUID);
    // find & by are keywords, which tell Spring what the Query is

}
