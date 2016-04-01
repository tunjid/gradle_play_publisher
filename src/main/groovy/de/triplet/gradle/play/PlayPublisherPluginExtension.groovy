package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    HashMap<String, String> serviceAccountEmails = new HashMap<>();

    HashMap<String, File> pk12Files = new HashMap<>();

    String serviceAccountEmail

    String buildTypeName

    File pk12File

    File jsonFile

    boolean uploadImages = false

    boolean errorOnSizeLimit = true

    private String track = 'alpha'

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', 'rollout' or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

    Double userFraction = 0.1

}
