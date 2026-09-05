const mongoose = require('mongoose');

const FloorSchema = new mongoose.Schema(
  {
    buildingId: {
      type: String,
      required: [true, 'Building identifier is required'],
      index: true
    },
    floorNumber: {
      type: Number,
      required: [true, 'Floor number is required']
    },
    name: {
      type: String,
      required: [true, 'Floor name is required'],
      trim: true
    },
    shortName: {
      type: String,
      required: true,
      default: 'G'
    },
    blueprintUrl: {
      type: String,
      default: ''
    },
    elevationMeters: {
      type: Number,
      default: 0
    }
  },
  {
    timestamps: true
  }
);

FloorSchema.index({ buildingId: 1, floorNumber: 1 }, { unique: true });

module.exports = mongoose.models.Floor || mongoose.model('Floor', FloorSchema);
