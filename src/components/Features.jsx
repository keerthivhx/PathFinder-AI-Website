import React from 'react';
import { 
  Compass, 
  BrainCircuit, 
  Layers, 
  Accessibility, 
  Navigation, 
  Search,
  CheckCircle,
  Sparkles
} from 'lucide-react';

const features = [
  {
    icon: Compass,
    title: "Smart Indoor Navigation",
    description: "Sub-meter indoor graph pathfinding using precise spatial waypoints, topological vectors, and inertial sensor dead reckoning.",
    accent: "from-cyan-500 to-blue-600",
    badge: "Sub-meter Accuracy",
    badgeColor: "text-cyan-400 bg-cyan-950/60 border-cyan-500/30",
  },
  {
    icon: BrainCircuit,
    title: "AI Route Optimization",
    description: "Integrated with Google Gemini to understand natural language requests (e.g. 'take me to the closest pediatric clinic' or 'CS 301 lab').",
    accent: "from-purple-500 to-pink-600",
    badge: "Gemini Powered",
    badgeColor: "text-purple-400 bg-purple-950/60 border-purple-500/30",
  },
  {
    icon: Layers,
    title: "Multi-Floor Guidance",
    description: "Multi-level floor transitions with intelligent elevator, ramp, and stairwell guidance, calculating optimal vertical transfers.",
    accent: "from-indigo-500 to-cyan-600",
    badge: "Elevator & Stairs",
    badgeColor: "text-indigo-400 bg-indigo-950/60 border-indigo-500/30",
  },
  {
    icon: Accessibility,
    title: "Accessibility-Friendly Routes",
    description: "Step-free wheelchair routes strictly omitting stairs, escalators, and steep inclines to guarantee ADA compliant paths.",
    accent: "from-amber-500 to-orange-600",
    badge: "Step-Free / ADA",
    badgeColor: "text-amber-400 bg-amber-950/60 border-amber-500/30",
  },
  {
    icon: Navigation,
    title: "Real-Time Turn-by-Turn",
    description: "Live step-by-step guidance cards, distance countdown meters, heading alignment compass, and offline TTS voice narration.",
    accent: "from-emerald-500 to-teal-600",
    badge: "Voice Guidance",
    badgeColor: "text-emerald-400 bg-emerald-950/60 border-emerald-500/30",
  },
  {
    icon: Search,
    title: "Intelligent Instant Search",
    description: "Instant autocomplete across faculty rooms, emergency exits, restrooms, cafeterias, and specialized diagnostic laboratories.",
    accent: "from-cyan-400 to-teal-500",
    badge: "Instant Autocomplete",
    badgeColor: "text-teal-400 bg-teal-950/60 border-teal-500/30",
  },
];

export default function Features() {
  return (
    <section id="features" className="py-24 bg-slate-900/40 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/80 border border-cyan-500/30 text-cyan-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Core Capabilities</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Everything You Need to Navigate Indoors
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            Built with algorithmic precision and spatial intelligence. Engineered for complex hospitals, universities, transit terminals, and shopping hubs.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
          {features.map((feature, idx) => {
            const Icon = feature.icon;
            return (
              <div 
                key={idx}
                className="group relative p-7 rounded-2xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-cyan-500/5"
              >
                <div className="flex items-center justify-between mb-6">
                  <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${feature.accent} p-0.5 flex items-center justify-center shadow-lg`}>
                    <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center group-hover:bg-transparent transition-colors">
                      <Icon className="w-6 h-6 text-white" />
                    </div>
                  </div>
                  <span className={`text-[11px] font-bold px-2.5 py-1 rounded-md border ${feature.badgeColor}`}>
                    {feature.badge}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-white mb-2.5 group-hover:text-cyan-300 transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-slate-400 leading-relaxed">
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
