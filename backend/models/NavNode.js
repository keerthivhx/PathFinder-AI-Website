const mongoose = require('mongoose');

const NavNodeSchema = new mongoose.Schema(
  {
    nodeId: {
      type: String,
      required: true,
      unique: true,
      index: true
    },
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    floor: {
      type: Number,
      required: true,
      index: true
    },
    x: {
      type: Number,
      required: true
    },
    y: {
      type: Number,
      required: true
    },
    name: {
      type: String,
      required: true,
      trim: true
    },
    code: {
      type: String,
      required: true,
      trim: true
    },
    category: {
      type: String,
      required: true,
      default: 'GENERAL'
    },
    description: {
      type: String,
      default: ''
    },
    keywords: {
      type: [String],
      default: []
    },
    isAccessible: {
      type: Boolean,
      default: true
    },
    isEmergencyExit: {
      type: Boolean,
      default: false
    },
    visualSignageHint: {
      type: String,
      default: ''
    },
    iconName: {
      type: String,
      default: 'place'
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.models.NavNode || mongoose.model('NavNode', NavNodeSchema);
