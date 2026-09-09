import React from 'react';
import { X, Printer, QrCode, CheckCircle, Clock, MapPin, AlertTriangle, ShieldCheck, Tractor } from 'lucide-react';
import { SlotBooking, Language } from '../types';

interface Props {
  booking: SlotBooking | null;
  isOpen: boolean;
  onClose: () => void;
  language: Language;
}

export const TokenPassModal: React.FC<Props> = ({ booking, isOpen, onClose, language }) => {
  if (!isOpen || !booking) return null;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
      <div className="bg-white rounded-3xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-200 my-4">
        {/* Modal Top Actions */}
        <div className="bg-[#1B4332] text-white px-5 py-3.5 flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-[#D4E09B] flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4" />
            Official APMC Gate Entry Pass
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 cursor-pointer transition-colors"
            >
              <Printer className="w-3.5 h-3.5 text-[#D4E09B]" />
              <span>Print</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-xl hover:bg-white/10 text-white/70 hover:text-white cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Printable Pass Layout */}
        <div className="p-6 bg-[#F3F6F1] printable-pass">
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm relative overflow-hidden">
            {/* Watermark badge */}
            <div className="absolute -right-8 -bottom-8 opacity-5 pointer-events-none">
              <Tractor className="w-56 h-56 text-[#1B4332]" />
            </div>

            {/* Official Header */}
            <div className="text-center pb-4 border-b-2 border-dashed border-slate-200">
              <p className="text-[10px] font-extrabold uppercase tracking-widest text-[#1B4332]">
                Madhya Pradesh State Agricultural Marketing Board
              </p>
              <h3 className="text-base font-bold text-slate-900 mt-0.5">
                {booking.mandiCenterName}
              </h3>
              <p className="text-xs text-slate-500 flex items-center justify-center gap-1 mt-0.5">
                <MapPin className="w-3 h-3 text-slate-400" />
                District: {booking.district} (M.P.)
              </p>
            </div>

            {/* Token Badge */}
            <div className="my-5 p-5 rounded-2xl bg-[#F3F6F1] border border-[#A3B18A]/40 text-center">
              <p className="text-[11px] font-bold text-[#1B4332] uppercase tracking-wider">
                Digital e-Token Number
              </p>
              <div className="text-3xl sm:text-4xl font-black text-[#1B4332] tracking-tight font-mono my-1">
                {booking.tokenNumber}
              </div>
              <div className="inline-flex items-center gap-1.5 bg-[#1B4332] text-white text-xs font-bold uppercase tracking-wider px-3.5 py-1 rounded-full mt-1.5">
                <Clock className="w-3.5 h-3.5 text-[#D4E09B]" />
                <span>Slot: {booking.timeSlot}</span>
              </div>
            </div>

            {/* Simulated QR & Barcode Section */}
            <div className="flex items-center justify-between p-3.5 bg-[#F3F6F1]/70 rounded-xl border border-slate-200/80 mb-4">
              <div className="flex items-center gap-3">
                <div className="w-16 h-16 bg-white p-1.5 rounded-lg border border-slate-300 shadow-xs flex items-center justify-center">
                  {/* High visual QR Code simulation with SVG */}
                  <svg className="w-full h-full text-slate-900" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M2 2h8v8H2V2zm2 2v4h4V4H4zm10-2h8v8h-8V2zm2 2v4h4V4h-4zM2 14h8v8H2v-8zm2 2v4h4v-4H4zm14 2h4v4h-4v-4zm-4-4h2v2h-2v-2zm4 0h2v2h-2v-2zm-4 4h2v2h-2v-2zm2-2h2v2h-2v-2z" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">Scan at Mandi Gate 2</p>
                  <p className="text-[10px] font-mono text-slate-500 break-all">{booking.qrCodeData}</p>
                  <span className="text-[10px] text-[#2D6A4F] font-semibold inline-flex items-center gap-1 mt-0.5">
                    <CheckCircle className="w-3 h-3" />
                    Tamper-proof Digital Gate Pass
                  </span>
                </div>
              </div>
            </div>

            {/* Details Grid */}
            <div className="grid grid-cols-2 gap-3 text-xs mb-4">
              <div className="p-3 rounded-xl bg-[#F3F6F1]/60 border border-slate-200/80">
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Farmer Name</p>
                <p className="font-bold text-slate-900 mt-0.5">{booking.farmerName}</p>
                <p className="text-[10px] text-slate-500 mt-0.5">{booking.farmerPhone}</p>
              </div>

              <div className="p-3 rounded-xl bg-[#F3F6F1]/60 border border-slate-200/80">
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Vehicle Details</p>
                <p className="font-bold text-slate-900 mt-0.5">{booking.vehicleNumber}</p>
                <p className="text-[10px] text-slate-500 capitalize">{booking.vehicleType.replace('_', ' ')}</p>
              </div>

              <div className="p-3 rounded-xl bg-[#F3F6F1]/60 border border-slate-200/80">
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Crop & Quantity</p>
                <p className="font-bold text-slate-900 mt-0.5">{booking.cropName}</p>
                <p className="text-[10px] text-[#2D6A4F] font-semibold">{booking.estimatedYieldQuintals} Quintals</p>
              </div>

              <div className="p-3 rounded-xl bg-[#F3F6F1]/60 border border-slate-200/80">
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Scheduled Date</p>
                <p className="font-bold text-slate-900 mt-0.5">{booking.scheduledDate}</p>
                <p className="text-[10px] text-slate-500">{booking.timeSlot}</p>
              </div>
            </div>

            {/* Instructions */}
            <div className="p-4 bg-amber-50/80 rounded-2xl border border-amber-200/80 text-[11px] text-amber-900 space-y-1.5">
              <div className="flex items-center gap-1.5 font-bold text-amber-950">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-700 shrink-0" />
                <span>Mandatory Mandi Entry Guidelines</span>
              </div>
              <ul className="list-disc list-inside text-amber-800 text-[10px] space-y-0.5 pl-1">
                <li>Arrive 15 minutes before slot window at designated Gate #2.</li>
                <li>Moisture content must be below 12.0% for Fair Average Quality (FAQ) grade.</li>
                <li>Carry Aadhaar card & bank passbook for direct bank transfer (DBT).</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-white border-t border-slate-100 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-600 hover:text-slate-800 cursor-pointer transition-colors"
          >
            Close
          </button>
          <button
            onClick={handlePrint}
            className="px-5 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white text-xs font-bold uppercase tracking-wider rounded-xl shadow-xs flex items-center gap-2 cursor-pointer transition-colors"
          >
            <Printer className="w-4 h-4 text-[#D4E09B]" />
            <span>Print Token Slip</span>
          </button>
        </div>
      </div>
    </div>
  );
};
