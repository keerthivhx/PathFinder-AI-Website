const mongoose = require('mongoose');

const LocationSchema = new mongoose.Schema(
  {
    locationId: {
      type: String,
      required: true,
      unique: true,
      index: true
    },
    nodeId: {
      type: String,
      required: true,
      index: true
    },
    buildingId: {
      type: String,
      required: true,
      index: true
    },
    floorNumber: {
      type: Number,
      required: true,
      index: true
    },
    name: {
      type: String,
      required: [true, 'Location name is required'],
      trim: true,
      index: true
    },
    code: {
      type: String,
      required: true,
      trim: true,
      index: true
    },
    category: {
      type: String,
      enum: [
        'ENTRANCE',
        'RECEPTION',
        'TRIAGE',
        'EMERGENCY',
        'DOCTOR_OFFICE',
        'RADIOLOGY',
        'CARDIOLOGY',
        'SURGERY',
        'PHARMACY',
        'LABORATORY',
        'CLASSROOM',
        'AUDITORIUM',
        'ACCOUNTS',
        'FACULTY',
        'LIBRARY',
        'AIRPORT_GATE',
        'SECURITY_CHECK',
        'BAGGAGE_CLAIM',
        'CHECK_IN',
        'DUTY_FREE',
        'FOOD_COURT',
        'STORE',
        'RESTROOM',
        'ELEVATOR',
        'STAIRS',
        'ESCALATOR',
        'EMERGENCY_EXIT',
        'KIOSK',
        'GENERAL'
      ],
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
    coordinates: {
      x: { type: Number, required: true },
      y: { type: Number, required: true }
    },
    openingHours: {
      type: String,
      default: '8:00 AM - 6:00 PM'
    },
    contactPhone: {
      type: String,
      default: ''
    }
  },
  {
    timestamps: true
  }
);

LocationSchema.index({ name: 'text', code: 'text', description: 'text', keywords: 'text' });

module.exports = mongoose.models.Location || mongoose.model('Location', LocationSchema);
