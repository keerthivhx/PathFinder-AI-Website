import React, { useState, useEffect } from 'react';
import { 
  Navigation, 
  MapPin, 
  Layers, 
  Accessibility, 
  Volume2, 
  Clock, 
  Route, 
  Sparkles,
  ArrowRight,
  RotateCcw,
  CheckCircle2,
  Building2
} from 'lucide-react';

const mockFacilities = [
  {
    id: 'univ',
    name: 'University Engineering Block',
    floors: ['Floor 1 (Ground)', 'Floor 2 (Labs)', 'Floor 3 (Offices)', 'Floor 4 (Auditorium)'],
    starts: ['Main Entrance Lobby (QR Code 01)', 'East Gate Parking Access', 'Cafeteria Breezeway'],
    destinations: [
      { name: 'CS-302 AI & Robotics Lab', floor: 'Floor 2 (Labs)', distance: '85m', estTime: '1m 15s', hasElevator: true },
      { name: 'Dean Office 305', floor: 'Floor 3 (Offices)', distance: '140m', estTime: '2m 10s', hasElevator: true },
      { name: 'Makerspace 3D Fab Lab', floor: 'Floor 1 (Ground)', distance: '45m', estTime: '40s', hasElevator: false },
      { name: 'Grand Auditorium', floor: 'Floor 4 (Auditorium)', distance: '210m', estTime: '3m 00s', hasElevator: true },
    ]
  },
  {
    id: 'hosp',
    name: 'Metropolitan General Hospital',
    floors: ['Level 1 (Trauma & ER)', 'Level 2 (Radiology & MRI)', 'Level 3 (Surgical Suites)'],
    starts: ['Emergency ER Intake', 'Ambulatory Entrance', 'Main Visitor Lobby'],
    destinations: [
      { name: 'Radiology / MRI Scanner 2', floor: 'Level 2 (Radiology & MRI)', distance: '95m', estTime: '1m 30s', hasElevator: true },
      { name: 'Cardiac Intensive Care (ICU)', floor: 'Level 3 (Surgical Suites)', distance: '160m', estTime: '2m 25s', hasElevator: true },
      { name: 'Outpatient Pharmacy', floor: 'Level 1 (Trauma & ER)', distance: '30m', estTime: '30s', hasElevator: false },
    ]
  },
  {
    id: 'air',
    name: 'Terminal 2 International Airport',
    floors: ['Level 1 (Arrivals & Baggage)', 'Level 2 (Departures & Gates)'],
    starts: ['Security Screening Checkpoint B', 'Transfer Desk Center', 'Duty Free Central'],
    destinations: [
      { name: 'Departure Gate A14', floor: 'Level 2 (Departures & Gates)', distance: '230m', estTime: '3m 15s', hasElevator: false },
      { name: 'VIP Sky Lounge', floor: 'Level 2 (Departures & Gates)', distance: '120m', estTime: '1m 45s', hasElevator: false },
      { name: 'Baggage Carousel 4', floor: 'Level 1 (Arrivals & Baggage)', distance: '180m', estTime: '2m 30s', hasElevator: true },
    ]
  }
];

export default function InteractiveMapPreview() {
  const [selectedFacilityIndex, setSelectedFacilityIndex] = useState(0);
  const currentFacility = mockFacilities[selectedFacilityIndex];

  const [selectedStart, setSelectedStart] = useState(currentFacility.starts[0]);
  const [selectedDestIndex, setSelectedDestIndex] = useState(0);
  const [wheelchairOnly, setWheelchairOnly] = useState(false);
  const [isNavigating, setIsNavigating] = useState(false);
  const [progress, setProgress] = useState(0);

  const selectedDest = currentFacility.destinations[selectedDestIndex] || currentFacility.destinations[0];

  // When facility changes, reset
  useEffect(() => {
    setSelectedStart(mockFacilities[selectedFacilityIndex].starts[0]);
    setSelectedDestIndex(0);
    setProgress(0);
    setIsNavigating(false);
  }, [selectedFacilityIndex]);

  // Simulation loop
  useEffect(() => {
    let interval;
    if (isNavigating) {
      interval = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            setIsNavigating(false);
            return 100;
          }
          return prev + 5;
        });
      }, 200);
    }
    return () => clearInterval(interval);
  }, [isNavigating]);

  const handleStartNav = () => {
    setProgress(0);
    setIsNavigating(true);
  };

  const handleReset = () => {
    setProgress(0);
    setIsNavigating(false);
  };

  // SVG Coordinates calculation for interactive visual path
  const pathCoordinates = [
    { x: 50, y: 220 },
    { x: 120, y: 220 },
    { x: 120, y: 140 },
    { x: 260, y: 140 },
    { x: 260, y: 70 },
    { x: 380, y: 70 }
  ];

  // Calculate position along path
  const numSegments = pathCoordinates.length - 1;
  const currentSegmentIndex = Math.min(
    Math.floor((progress / 100) * numSegments),
    numSegments - 1
  );
  const segmentProgress = ((progress / 100) * numSegments) - currentSegmentIndex;
  
  const p1 = pathCoordinates[currentSegmentIndex];
  const p2 = pathCoordinates[currentSegmentIndex + 1];
  const markerX = p1.x + (p2.x - p1.x) * segmentProgress;
  const markerY = p1.y + (p2.y - p1.y) * segmentProgress;

  return (
    <section id="interactive-demo" className="py-24 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/70 border border-cyan-500/30 text-cyan-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Route className="w-3.5 h-3.5" />
            <span>Interactive Wayfinding Simulator</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
            Experience Sub-Meter Indoor Guidance
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            Test how the A* graph algorithm dynamically avoids stairs for wheelchairs, calculates vertical elevator shifts, and streams step-by-step turn guidance.
          </p>
        </div>

        {/* Facility Selector Tabs */}
        <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3 mb-10">
          {mockFacilities.map((fac, idx) => (
            <button
              key={fac.id}
              onClick={() => setSelectedFacilityIndex(idx)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                selectedFacilityIndex === idx
                  ? 'bg-cyan-500 text-slate-950 shadow-lg shadow-cyan-500/20'
                  : 'bg-slate-900/90 text-slate-400 hover:text-white border border-slate-800'
              }`}
            >
              <Building2 className="w-4 h-4" />
              <span>{fac.name}</span>
            </button>
          ))}
        </div>

        {/* Interactive Workspace Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Controls Panel (5 cols) */}
          <div className="lg:col-span-5 bg-slate-900/90 rounded-2xl border border-slate-800 p-6 shadow-xl flex flex-col gap-5">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Navigation className="w-4 h-4 text-cyan-400" />
                <span>Route Parameters</span>
              </h3>
              <span className="text-xs font-mono text-cyan-400 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-500/30">
                A* Router Ready
              </span>
            </div>

            {/* Starting Point */}
            <div>
              <label className="text-xs font-semibold text-slate-400 block mb-2 uppercase tracking-wider">
                Starting Location
              </label>
              <select
                value={selectedStart}
                onChange={(e) => setSelectedStart(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
              >
                {currentFacility.starts.map((st, i) => (
                  <option key={i} value={st}>{st}</option>
                ))}
              </select>
            </div>

            {/* Destination */}
            <div>
              <label className="text-xs font-semibold text-slate-400 block mb-2 uppercase tracking-wider">
                Destination Target
              </label>
              <select
                value={selectedDestIndex}
                onChange={(e) => setSelectedDestIndex(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
              >
                {currentFacility.destinations.map((dst, i) => (
                  <option key={i} value={i}>{dst.name} ({dst.floor})</option>
                ))}
              </select>
            </div>

            {/* Wheelchair toggle */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800">
              <div className="flex items-center gap-2.5">
                <Accessibility className="w-5 h-5 text-amber-400" />
                <div>
                  <div className="text-sm font-semibold text-slate-200">Step-Free Accessibility</div>
                  <div className="text-[11px] text-slate-400">Strictly elevators & ramps only</div>
                </div>
              </div>
              <input
                type="checkbox"
                checked={wheelchairOnly}
                onChange={(e) => setWheelchairOnly(e.target.checked)}
                className="w-4 h-4 rounded text-cyan-500 bg-slate-900 border-slate-700 focus:ring-cyan-500"
              />
            </div>

            {/* Route Stats Summary */}
            <div className="grid grid-cols-3 gap-2.5 p-3 rounded-xl bg-slate-950/80 border border-slate-800">
              <div className="text-center">
                <span className="text-[11px] text-slate-400 block">Distance</span>
                <span className="text-sm font-bold text-cyan-400">{selectedDest.distance}</span>
              </div>
              <div className="text-center border-x border-slate-800">
                <span className="text-[11px] text-slate-400 block">Walk Time</span>
                <span className="text-sm font-bold text-purple-400">{selectedDest.estTime}</span>
              </div>
              <div className="text-center">
                <span className="text-[11px] text-slate-400 block">Vertical Shift</span>
                <span className="text-sm font-bold text-emerald-400">
                  {selectedDest.hasElevator ? (wheelchairOnly ? 'Elevator (ADA)' : 'Elevator / Stairs') : 'Same Level'}
                </span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-3 pt-2">
              <button
                onClick={handleStartNav}
                disabled={isNavigating}
                className="flex-1 inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-teal-400 text-slate-950 font-bold text-sm hover:opacity-90 active:scale-98 transition-all disabled:opacity-50"
              >
                <Navigation className="w-4 h-4 text-slate-950" />
                <span>{isNavigating ? 'Simulating Walk...' : 'Simulate Navigation'}</span>
              </button>

              <button
                onClick={handleReset}
                title="Reset simulation"
                className="p-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
              >
                <RotateCcw className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Live Map Canvas Simulation (7 cols) */}
          <div className="lg:col-span-7 bg-slate-900/90 rounded-2xl border border-slate-800 p-6 shadow-xl flex flex-col">
            {/* Live Navigation Banner */}
            <div className="mb-4 p-4 rounded-xl bg-gradient-to-r from-slate-950 via-slate-900 to-slate-950 border border-cyan-500/30 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-cyan-500/20 text-cyan-400 flex items-center justify-center font-bold">
                  <ArrowRight className="w-5 h-5 text-cyan-400" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-cyan-400 uppercase tracking-wider">
                    {progress === 100 ? 'You Have Arrived!' : (progress > 50 ? 'Turn Right toward Elevator' : 'Proceed Straight 25m')}
                  </p>
                  <p className="text-sm font-bold text-white">
                    {progress === 100 ? selectedDest.name : `Heading to ${selectedDest.name}`}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 text-xs text-slate-400 bg-slate-800/80 px-2.5 py-1 rounded-md">
                <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
                <span>Voice Active</span>
              </div>
            </div>

            {/* SVG Indoor Topological Map */}
            <div className="relative w-full h-72 sm:h-80 bg-slate-950 rounded-xl border border-slate-800 overflow-hidden flex items-center justify-center p-4">
              {/* Floor grid pattern */}
              <div 
                className="absolute inset-0 opacity-15"
                style={{
                  backgroundImage: `radial-gradient(#06b6d4 1px, transparent 1px)`,
                  backgroundSize: '24px 24px'
                }}
              />

              <svg viewBox="0 0 450 260" className="w-full h-full">
                {/* Rooms and Corridors Background */}
                <rect x="20" y="20" width="410" height="220" rx="12" fill="#0f172a" stroke="#1e293b" strokeWidth="2" />
                
                {/* Corridors */}
                <path d="M 40 220 L 400 220" stroke="#1e293b" strokeWidth="24" strokeLinecap="round" />
                <path d="M 120 220 L 120 40" stroke="#1e293b" strokeWidth="24" strokeLinecap="round" />
                <path d="M 120 140 L 380 140" stroke="#1e293b" strokeWidth="24" strokeLinecap="round" />
                <path d="M 260 140 L 260 40" stroke="#1e293b" strokeWidth="24" strokeLinecap="round" />
                <path d="M 260 70 L 400 70" stroke="#1e293b" strokeWidth="24" strokeLinecap="round" />

                {/* Room blocks */}
                <rect x="30" y="40" width="70" height="60" rx="6" fill="#1e293b" opacity="0.6" />
                <text x="65" y="75" fill="#64748b" fontSize="9" textAnchor="middle">Room 101</text>

                <rect x="140" y="40" width="100" height="80" rx="6" fill="#1e293b" opacity="0.6" />
                <text x="190" y="85" fill="#64748b" fontSize="10" textAnchor="middle">Lab Studio</text>

                <rect x="280" y="40" width="120" height="60" rx="6" fill="#06b6d4" fillOpacity="0.15" stroke="#06b6d4" strokeWidth="1" strokeDasharray="3 3" />
                <text x="340" y="75" fill="#38bdf8" fontSize="10" fontWeight="bold" textAnchor="middle">Target Lab</text>

                <rect x="140" y="160" width="100" height="45" rx="6" fill="#1e293b" opacity="0.6" />
                <text x="190" y="187" fill="#64748b" fontSize="9" textAnchor="middle">Offices</text>

                {/* Elevator Node */}
                <circle cx="260" cy="140" r="14" fill="#6366f1" fillOpacity="0.3" stroke="#818cf8" strokeWidth="2" />
                <text x="260" y="143" fill="#c7d2fe" fontSize="8" fontWeight="bold" textAnchor="middle">ELEV</text>

                {/* Planned Path Line (Dashed) */}
                <polyline
                  points="50,220 120,220 120,140 260,140 260,70 380,70"
                  fill="none"
                  stroke="#334155"
                  strokeWidth="6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />

                {/* Active Path Line (Cyan Glow) */}
                <polyline
                  points="50,220 120,220 120,140 260,140 260,70 380,70"
                  fill="none"
                  stroke="#06b6d4"
                  strokeWidth="4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeDasharray="400"
                  strokeDashoffset={400 - (400 * (progress / 100))}
                />

                {/* Start Marker */}
                <circle cx="50" cy="220" r="7" fill="#10b981" stroke="#ffffff" strokeWidth="2" />
                <text x="50" y="242" fill="#34d399" fontSize="9" fontWeight="bold" textAnchor="middle">START</text>

                {/* End Marker */}
                <circle cx="380" cy="70" r="7" fill="#ef4444" stroke="#ffffff" strokeWidth="2" />
                <text x="380" y="92" fill="#f87171" fontSize="9" fontWeight="bold" textAnchor="middle">GOAL</text>

                {/* Real-time Walking Avatar Marker */}
                <g transform={`translate(${markerX}, ${markerY})`}>
                  <circle r="12" fill="#06b6d4" fillOpacity="0.4" className="animate-ping" />
                  <circle r="6" fill="#38bdf8" stroke="#ffffff" strokeWidth="2" />
                </g>
              </svg>

              {/* Floor Level Indicator */}
              <div className="absolute top-4 left-4 bg-slate-900/90 border border-slate-800 rounded-lg px-2.5 py-1 text-xs font-mono text-cyan-400">
                Active: {selectedDest.floor}
              </div>
            </div>

            {/* Progress bar */}
            <div className="mt-4 pt-3 border-t border-slate-800 flex items-center gap-4">
              <div className="flex-1 bg-slate-950 h-2 rounded-full overflow-hidden border border-slate-800">
                <div 
                  className="h-full bg-gradient-to-r from-cyan-500 to-teal-400 transition-all duration-300"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <span className="text-xs font-mono text-slate-400 min-w-[3rem] text-right">
                {progress}%
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
