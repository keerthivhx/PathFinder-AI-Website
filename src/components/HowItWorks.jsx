import React from 'react';
import { Search, QrCode, Route, Compass, Sparkles } from 'lucide-react';

const steps = [
  {
    step: "01",
    title: "Search Destination",
    desc: "Type any department, room code, faculty name, clinical symptom, or plain-English intent into the AI search bar.",
    icon: Search,
    color: "from-cyan-500 to-blue-600"
  },
  {
    step: "02",
    title: "Set Starting Point",
    desc: "Scan an entrance QR code, snap a photo of a doorway marker, or tap your initial waypoint on the topological map.",
    icon: QrCode,
    color: "from-purple-500 to-pink-600"
  },
  {
    step: "03",
    title: "AI Calculates Route",
    desc: "A* spatial graph algorithm computes the fastest topological path, balancing elevator waits, distance, and stairs.",
    icon: Route,
    color: "from-indigo-500 to-cyan-600"
  },
  {
    step: "04",
    title: "Live Turn-by-Turn Guidance",
    desc: "Follow animated directional cues, distance countdowns, and real-time text-to-speech voice narration to your goal.",
    icon: Compass,
    color: "from-emerald-500 to-teal-600"
  }
];

export default function HowItWorks() {
  return (
    <section id="how-it-works" className="py-24 bg-slate-900/50 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-950/70 border border-purple-500/30 text-purple-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Intuitive Workflow</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
            4 Simple Steps to Your Destination
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            No complicated pairing or external Bluetooth setup required. Start walking right away.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div 
                key={idx}
                className="relative p-6 rounded-2xl bg-slate-900/90 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition-all hover:-translate-y-1"
              >
                {/* Step badge */}
                <div className="flex items-center justify-between mb-6">
                  <span className="text-xs font-black font-mono px-3 py-1 rounded-lg bg-slate-950 border border-slate-800 text-cyan-400">
                    STEP {item.step}
                  </span>
                  <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${item.color} flex items-center justify-center text-white shadow-md`}>
                    <Icon className="w-5 h-5" />
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-bold text-white mb-2">{item.title}</h3>
                  <p className="text-sm text-slate-400 leading-relaxed">{item.desc}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
