const mongoose = require('mongoose');

const NavigationSessionSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null
    },
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    startNodeId: {
      type: String,
      required: true
    },
    targetNodeId: {
      type: String,
      required: true
    },
    isWheelchairMode: {
      type: Boolean,
      default: false
    },
    isEmergencyMode: {
      type: Boolean,
      default: false
    },
    totalDistanceMeters: {
      type: Number,
      required: true
    },
    estimatedTimeSeconds: {
      type: Number,
      required: true
    },
    floorsTraversed: {
      type: [Number],
      default: []
    },
    stepCount: {
      type: Number,
      default: 0
    },
    status: {
      type: String,
      enum: ['STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
      default: 'STARTED'
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.models.NavigationSession || mongoose.model('NavigationSession', NavigationSessionSchema);
