const mongoose = require('mongoose');

const BuildingSchema = new mongoose.Schema(
  {
    buildingId: {
      type: String,
      required: [true, 'Building identifier is required'],
      unique: true,
      trim: true,
      index: true
    },
    name: {
      type: String,
      required: [true, 'Building name is required'],
      trim: true,
      maxlength: 150
    },
    type: {
      type: String,
      enum: ['University', 'Hospital', 'Airport', 'Shopping Mall', 'Corporate', 'Government', 'Custom'],
      default: 'University'
    },
    description: {
      type: String,
      default: ''
    },
    address: {
      type: String,
      default: ''
    },
    totalFloors: {
      type: Number,
      default: 4
    },
    activeStatus: {
      type: Boolean,
      default: true
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.models.Building || mongoose.model('Building', BuildingSchema);
