package com.artifactalley.bid;

public class ArtifactNotFoundException extends BiddingException {
    public ArtifactNotFoundException() {
        super("That artifact could not be found.");
    }
}
