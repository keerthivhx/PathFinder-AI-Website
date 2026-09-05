const mongoose = require('mongoose');

const FavoriteSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true
    },
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    nodeId: {
      type: String,
      required: true
    },
    customLabel: {
      type: String,
      default: ''
    }
  },
  {
    timestamps: true
  }
);

FavoriteSchema.index({ userId: 1, buildingId: 1, nodeId: 1 }, { unique: true });

module.exports = mongoose.models.Favorite || mongoose.model('Favorite', FavoriteSchema);
