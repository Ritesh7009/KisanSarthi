import React from 'react';
import { Info, Sparkles } from 'lucide-react';
import { Language } from '../types';

interface Props {
  language: Language;
}

export const SyntheticDataBanner: React.FC<Props> = ({ language }) => {
  return (
    <div className="bg-white/90 border-b border-slate-200/80 px-4 py-2 text-xs text-slate-700 flex flex-wrap items-center justify-between gap-2 shadow-2xs backdrop-blur-xs">
      <div className="flex items-center gap-2.5">
        <span className="inline-flex items-center gap-1 bg-[#D4E09B]/35 text-[#1B4332] font-bold px-2.5 py-0.5 rounded-full text-[10px] tracking-wide uppercase border border-[#A3B18A]/40">
          <Sparkles className="w-3 h-3 text-[#2D6A4F]" />
          SIH Prototype Demo
        </span>
        <span className="text-slate-600 text-[11px] sm:text-xs">
          {language === 'hi' ? (
            <>
              <strong>पारदर्शिता सूचना:</strong> मंडी केंद्र (सीहोर, हरदा, उज्जैन आदि) एवं समर्थन मूल्य (MSP) म.प्र. शासन के वास्तविक मानकों पर आधारित हैं। किसान पहचान एवं वाहन संख्या परीक्षण हेतु सिमुलेटेड (डेमो) हैं।
            </>
          ) : language === 'mal' ? (
            <>
              <strong>डेमो जानकारी:</strong> मंडी अर सरकारी भाव (MSP) म.प्र. का असली हैं। किसान का नाम अर गाड़ी नंबर जांच वास्ते डेमो हे।
            </>
          ) : (
            <>
              <strong>Transparency Notice:</strong> Mandi centers (Sehore, Harda, Ujjain, etc.) and MSP rates reflect official MP government benchmarks. Farmer identities, vehicle numbers, and tokens are synthetic demo data for SIH evaluation.
            </>
          )}
        </span>
      </div>
      <div className="hidden sm:flex items-center gap-2 text-slate-400 font-mono text-[10px]">
        <Info className="w-3.5 h-3.5 text-[#2D6A4F]" />
        <span>APMC MP e-Uparjan v2.4</span>
      </div>
    </div>
  );
};
