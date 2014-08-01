package at.tomtasche.nomstagram.engine;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

import javax.inject.Named;

/**
 * An endpoint class we are exposing
 */
@Api(name = "foodApi", version = "v1", namespace = @ApiNamespace(ownerDomain = "engine.nomstagram.tomtasche.at", ownerName = "engine.nomstagram.tomtasche.at", packagePath = ""))
public class FoodEndpoint {

    private final AsyncDatastoreService datastoreService;

    public FoodEndpoint() {
        datastoreService = DatastoreServiceFactory.getAsyncDatastoreService();
    }

    @ApiMethod(name = "keywords")
    public void addKeywords(@Named("keywords") String newKeywords) {
        Iterable<Entity> keywords = FoodHelper.getAllKeywords(datastoreService);

        String[] splitKeywords = newKeywords.split(",");
        for (int i = 0; i < splitKeywords.length; i++) {
            Entity keywordEntity = new Entity("keyword");
            keywordEntity.setProperty("name", splitKeywords[i]);

            datastoreService.put(keywordEntity);
        }
    }
}