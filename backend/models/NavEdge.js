const mongoose = require('mongoose');

const NavEdgeSchema = new mongoose.Schema(
  {
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    fromId: {
      type: String,
      required: true,
      index: true
    },
    toId: {
      type: String,
      required: true,
      index: true
    },
    distanceMeters: {
      type: Number,
      required: true,
      min: 0.1
    },
    edgeType: {
      type: String,
      enum: ['CORRIDOR', 'ELEVATOR', 'STAIRS', 'ESCALATOR', 'RAMP', 'EMERGENCY_EXIT'],
      default: 'CORRIDOR'
    },
    isWheelchairAccessible: {
      type: Boolean,
      default: true
    },
    bidirectional: {
      type: Boolean,
      default: true
    }
  },
  {
    timestamps: true
  }
);

NavEdgeSchema.index({ buildingId: 1, fromId: 1, toId: 1 });

module.exports = mongoose.models.NavEdge || mongoose.model('NavEdge', NavEdgeSchema);
