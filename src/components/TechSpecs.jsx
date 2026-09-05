import React from 'react';
import { Cpu, Database, Server, Smartphone, Shield, Zap } from 'lucide-react';

const specs = [
  {
    icon: Cpu,
    title: "Graph Engine Core",
    tech: "Algorithmic A* & Spatial Graphs",
    details: "Calculates sub-second topological paths with 3D coordinate distance heuristics, elevator penalisations, and stair avoidance."
  },
  {
    icon: Zap,
    title: "AI Intent Engine",
    tech: "Google Gemini Flash API",
    details: "Translates conversational prompts like 'I feel dizzy and need urgent attention' directly into emergency ER room waypoints."
  },
  {
    icon: Smartphone,
    title: "Sensor Fusion",
    tech: "PDR + BLE + QR Calibration",
    details: "Blends Pedestrian Dead Reckoning (accelerometer/gyroscope) with Bluetooth beacon trilateration and QR position resets."
  },
  {
    icon: Database,
    title: "Offline Storage",
    tech: "Encrypted Room SQLite",
    details: "Caches multi-floor architectural blueprints and navigation graphs locally on device for complete offline operation in basements."
  },
  {
    icon: Server,
    title: "Production Backend",
    tech: "REST + GeoJSON APIs",
    details: "High-throughput cloud synchronization for campus administrators to upload floorplans, edit waypoints, and inspect heatmaps."
  },
  {
    icon: Shield,
    title: "Privacy First",
    tech: "Zero Telemetry Leaks",
    details: "Personal path traces and step trajectories stay on the user's phone, complying with strict healthcare and educational privacy guidelines."
  }
];

export default function TechSpecs() {
  return (
    <section id="specs" className="py-24 relative bg-slate-900/40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/70 border border-cyan-500/30 text-cyan-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Cpu className="w-3.5 h-3.5" />
            <span>Architecture & Specs</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
            Engineered for Industrial Reliability
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            High-performance indoor positioning built with modern Android engineering, reactive architecture, and local edge computing.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {specs.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div 
                key={idx}
                className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 transition-all"
              >
                <div className="w-10 h-10 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center text-cyan-400 mb-4">
                  <Icon className="w-5 h-5" />
                </div>
                <div className="text-xs font-mono text-cyan-400 font-semibold mb-1">{item.tech}</div>
                <h3 className="text-base font-bold text-white mb-2">{item.title}</h3>
                <p className="text-xs text-slate-400 leading-relaxed">{item.details}</p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
