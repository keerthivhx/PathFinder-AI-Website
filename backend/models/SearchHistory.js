const mongoose = require('mongoose');

const SearchHistorySchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
      index: true
    },
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    query: {
      type: String,
      required: true,
      trim: true
    },
    selectedNodeId: {
      type: String,
      default: null
    },
    resultCount: {
      type: Number,
      default: 0
    }
  },
  {
    timestamps: true
  }
);

SearchHistorySchema.index({ buildingId: 1, createdAt: -1 });

module.exports = mongoose.models.SearchHistory || mongoose.model('SearchHistory', SearchHistorySchema);
