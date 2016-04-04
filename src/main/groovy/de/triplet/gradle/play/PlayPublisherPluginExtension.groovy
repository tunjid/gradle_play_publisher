package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    /**
     * <p>A mapping of service account emails for each variant.</p>
     * <p>If a variant is not mapped here, the plugin will default to
     * {@link PlayPublisherPluginExtension#serviceAccountEmail}</p>
     */
    HashMap<String, String> serviceAccountEmails = new HashMap<>();

    /**
     * <p>A mapping of pk12 files for each variant.</p>
     * <p>If a variant is not mapped here, the plugin will default to
     * {@link PlayPublisherPluginExtension#pk12File}</p>
     */
    HashMap<String, File> pk12Files = new HashMap<>();

    String serviceAccountEmail

    String buildType

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
