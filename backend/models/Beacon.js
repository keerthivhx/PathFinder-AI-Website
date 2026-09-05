const mongoose = require('mongoose');

const BeaconSchema = new mongoose.Schema(
  {
    beaconId: {
      type: String,
      required: [true, 'Beacon identifier is required (e.g. BEACON-GF-01)'],
      unique: true,
      trim: true,
      index: true
    },
    buildingId: {
      type: String,
      required: [true, 'Associated buildingId is required'],
      index: true
    },
    floorId: {
      type: Number,
      required: [true, 'Floor number is required (0 for Ground, 1 for Floor 1, etc.)'],
      default: 0
    },
    nodeId: {
      type: String,
      required: [true, 'Associated navigation NavNode id is required (e.g. univ_gate)'],
      index: true
    },
    x: {
      type: Number,
      required: [true, 'X coordinate on normalized 0..1000 canvas is required'],
      default: 500
    },
    y: {
      type: Number,
      required: [true, 'Y coordinate on normalized 0..1000 canvas is required'],
      default: 500
    },
    major: {
      type: Number,
      default: 1
    },
    minor: {
      type: Number,
      default: 1
    },
    txPower: {
      type: Number,
      default: -59 // Measured RSSI at 1 meter in dBm
    },
    activeStatus: {
      type: Boolean,
      default: true
    },
    metadata: {
      macAddress: { type: String, default: '' },
      batteryLevel: { type: Number, default: 100 },
      description: { type: String, default: 'PathFinder BLE Anchor Beacon' }
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.models.Beacon || mongoose.model('Beacon', BeaconSchema);
