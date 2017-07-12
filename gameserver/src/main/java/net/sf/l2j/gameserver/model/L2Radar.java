package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.RadarControl;

import java.util.ArrayList;
import java.util.List;

public final class L2Radar {
    private final L2PcInstance player;
    private final List<RadarMarker> markers;

    public L2Radar(L2PcInstance player) {
        this.player = player;
        this.markers = new ArrayList<>();
    }

    public void addMarker(int x, int y, int z) {
        RadarMarker newMarker = new RadarMarker(x, y, z);
        markers.add(newMarker);
        player.sendPacket(new RadarControl(2, 2, x, y, z));
        player.sendPacket(new RadarControl(0, 1, x, y, z));
    }

    public void removeMarker(int x, int y, int z) {
        RadarMarker newMarker = new RadarMarker(x, y, z);
        markers.remove(newMarker);
        player.sendPacket(new RadarControl(1, 1, x, y, z));
    }

    public void removeAllMarkers() {
        for (RadarMarker tempMarker : markers) {
            player.sendPacket(new RadarControl(2, 2, tempMarker.posX, tempMarker.posY, tempMarker.posZ));
        }

        markers.clear();
    }

    public void loadMarkers() {
        player.sendPacket(new RadarControl(2, 2, player.getX(), player.getY(), player.getZ()));
        for (RadarMarker tempMarker : markers) {
            player.sendPacket(new RadarControl(0, 1, tempMarker.posX, tempMarker.posY, tempMarker.posZ));
        }
    }

    private static final class RadarMarker {
        private final int type;
        private final int posX;
        private final int posY;
        private final int posZ;

        private RadarMarker(int type, int posX, int posY, int posZ) {
            this.type = type;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        private RadarMarker(int posX, int posY, int posZ) {
            type = 1;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + type;
            result = prime * result + posX;
            result = prime * result + posY;
            result = prime * result + posZ;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj == null) { return false; }
            if (!(obj instanceof RadarMarker)) { return false; }
            RadarMarker other = (RadarMarker) obj;
            if (type != other.type) { return false; }
            if (posX != other.posX) { return false; }
            if (posY != other.posY) { return false; }

            return posZ == other.posZ;

        }
    }

    @Override
    public String toString() {
        return "L2Radar{" +
                "player=" + player +
                ", markers=" + markers +
                '}';
    }
}