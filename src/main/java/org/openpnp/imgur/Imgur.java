package org.openpnp.imgur;

import java.io.File;

import org.pmw.tinylog.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class Imgur {
    private final String clientId;
    
    public Imgur(String clientId) {
        this.clientId = clientId;
    }
    
    public Image uploadImage(File file) throws Exception {
        // {"data":{"bandwidth":0,"nsfw":null,"is_ad":false,"link":"http://i.imgur.com/4a2U4HI.png","description":null,"section":null,"title":null,"type":"image/png","tags":[],"deletehash":"g5KtSKSSXT2l54Q","datetime":1490023631,"account_id":0,"size":258,"width":35,"account_url":null,"name":"","animated":false,"id":"4a2U4HI","in_gallery":false,"vote":null,"favorite":false,"views":0,"height":35},"success":true,"status":200}
        HttpResponse<JsonNode> response = Unirest
                .post("https://api.imgur.com/3/image")
                .header("accept", "application/json")
                .header("Authorization", "Client-ID " + clientId)
                .field("image", file)
                .field("name", file.getName())
                .field("title", file.getName())
                .asJson();
        if (!response.getBody().getObject().getBoolean("success")) {
            Logger.debug(response.getBody().toString());
            throw new Exception(response.getBody().getObject().getJSONObject("data").getString("error"));
        }
        Image image = new Image();
        image.id = response.getBody().getObject().getJSONObject("data").getString("id");
        image.deleteHash = response.getBody().getObject().getJSONObject("data").getString("deletehash");
        return image; 
    }
    
    public Album createAlbum(String description, Image... images) throws Exception {
        String[] imageDeleteHashes = new String[images.length];
        for (int i = 0; i < images.length; i++) {
            imageDeleteHashes[i] = images[i].deleteHash;
        }
        // {"data":{"id":"ZrUmj","deletehash":"m2BpMxDliK0FEgk"},"success":true,"status":200}
        HttpResponse<JsonNode> response = Unirest
            .post("https://api.imgur.com/3/album")
            .header("accept", "application/json")
            .header("Authorization", "Client-ID " + clientId)
            .field("description", description)
            .field("privacy", "hidden")
            .field("deletehashes", String.join(",", imageDeleteHashes))
            .asJson();
        if (!response.getBody().getObject().getBoolean("success")) {
            Logger.debug(response.getBody().toString());
            throw new Exception(response.getBody().getObject().getJSONObject("data").getString("error"));
        }
        Album album = new Album();
        album.id = response.getBody().getObject().getJSONObject("data").getString("id");
        album.deleteHash = response.getBody().getObject().getJSONObject("data").getString("deletehash");
        return album;
    }
    
    public static class Image {
        public String id;
        public String deleteHash;
    }
    
    public static class Album {
        public String id;
        public String deleteHash;
    }
}
