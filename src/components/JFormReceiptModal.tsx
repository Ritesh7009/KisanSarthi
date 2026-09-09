import React from 'react';
import { X, Printer, CheckCircle2, ShieldCheck, FileText, Building2, Banknote } from 'lucide-react';
import { SlotBooking, Language } from '../types';

interface Props {
  booking: SlotBooking | null;
  isOpen: boolean;
  onClose: () => void;
  language: Language;
}

export const JFormReceiptModal: React.FC<Props> = ({ booking, isOpen, onClose, language }) => {
  if (!isOpen || !booking) return null;

  const gross = booking.actualGrossWeightKg || (booking.estimatedYieldQuintals * 100 + 3800);
  const tare = booking.actualTareWeightKg || 3800;
  const netKg = gross - tare;
  const netQuintals = booking.netWeightQuintals || (netKg / 100);
  const payout = booking.totalPayoutRs || (netQuintals * 2400);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
      <div className="bg-white rounded-3xl shadow-2xl max-w-xl w-full overflow-hidden border border-slate-200 my-4">
        {/* Header bar */}
        <div className="bg-[#1B4332] text-white px-5 py-4 flex items-center justify-between border-b border-[#2D6A4F]">
          <div className="flex items-center gap-2.5">
            <FileText className="w-5 h-5 text-[#D4E09B]" />
            <span className="text-xs font-bold uppercase tracking-wider text-white">
              Official Mandi Procurement Receipt (J-Form / तौल पर्ची)
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => window.print()}
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 cursor-pointer transition-colors"
            >
              <Printer className="w-3.5 h-3.5 text-[#D4E09B]" />
              <span>Print Slip</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-xl hover:bg-white/10 text-white/70 hover:text-white cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Official Slip Content */}
        <div className="p-6 bg-[#F3F6F1] text-slate-900 text-xs printable-jform">
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            {/* Header Stamp */}
            <div className="text-center pb-3 border-b border-slate-200">
              <div className="flex items-center justify-center gap-2 mb-1">
                <Building2 className="w-5 h-5 text-[#1B4332]" />
                <p className="text-xs font-black uppercase tracking-widest text-[#1B4332]">
                  Madhya Pradesh State Agricultural Marketing Board
                </p>
              </div>
              <h3 className="text-sm font-bold text-slate-900">
                {booking.mandiCenterName}
              </h3>
              <p className="text-[11px] text-slate-500 mt-0.5">
                Form 'J' (Rule 42) • Computerized Weighment & Procurement Voucher
              </p>
            </div>

            {/* Receipt Meta */}
            <div className="grid grid-cols-2 gap-2 text-[11px] bg-[#F3F6F1] p-3 rounded-xl border border-slate-200/80">
              <div>
                <span className="text-slate-400 font-bold uppercase tracking-wider text-[10px]">Receipt No: </span>
                <strong className="font-mono text-slate-900 block mt-0.5">JF-MP-{booking.tokenNumber}</strong>
              </div>
              <div className="text-right">
                <span className="text-slate-400 font-bold uppercase tracking-wider text-[10px]">Date & Time: </span>
                <strong className="text-slate-900 block mt-0.5">{new Date(booking.createdAt).toLocaleDateString()} {booking.timeSlot.split('-')[0]}</strong>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase tracking-wider text-[10px]">Farmer: </span>
                <strong className="text-slate-900 block mt-0.5">{booking.farmerName}</strong>
              </div>
              <div className="text-right">
                <span className="text-slate-400 font-bold uppercase tracking-wider text-[10px]">Vehicle: </span>
                <strong className="font-mono text-slate-900 block mt-0.5">{booking.vehicleNumber}</strong>
              </div>
            </div>

            {/* Weighbridge Computation Table */}
            <div>
              <p className="text-[11px] font-bold text-slate-700 uppercase tracking-wider mb-2">
                Weighment & Quality Assessment
              </p>
              <div className="overflow-hidden rounded-xl border border-slate-200">
                <table className="w-full border-collapse text-left">
                  <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-600">
                    <tr>
                      <th className="p-2.5 border-b border-slate-200">Parameter</th>
                      <th className="p-2.5 border-b border-slate-200 text-right">Value Recorded</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 font-mono text-[11px]">
                    <tr>
                      <td className="p-2.5 font-sans font-medium text-slate-700">Gross Loaded Weight (Gross WT)</td>
                      <td className="p-2.5 text-right font-bold text-slate-900">{gross.toLocaleString()} kg</td>
                    </tr>
                    <tr>
                      <td className="p-2.5 font-sans font-medium text-slate-700">Empty Vehicle Weight (Tare WT)</td>
                      <td className="p-2.5 text-right font-bold text-slate-900">{tare.toLocaleString()} kg</td>
                    </tr>
                    <tr className="bg-[#D4E09B]/25">
                      <td className="p-2.5 font-sans font-bold text-[#1B4332]">Net Commodity Weight</td>
                      <td className="p-2.5 text-right font-bold text-[#1B4332]">{netKg.toLocaleString()} kg ({netQuintals.toFixed(2)} Qtl)</td>
                    </tr>
                    <tr>
                      <td className="p-2.5 font-sans text-slate-600">Moisture Content</td>
                      <td className="p-2.5 text-right text-slate-700">{booking.moisturePct || 10.4}% (FAQ Limit: ≤12.0%)</td>
                    </tr>
                    <tr>
                      <td className="p-2.5 font-sans text-slate-600">Quality Inspection Grade</td>
                      <td className="p-2.5 text-right font-bold text-[#2D6A4F]">
                        <span className="px-2 py-0.5 rounded-md bg-[#D4E09B]/40 text-[#1B4332] text-[10px] font-bold">
                          {booking.qualityGrade || 'Grade A (FAQ)'}
                        </span>
                      </td>
                    </tr>
                    <tr className="bg-[#F3F6F1]/60">
                      <td className="p-2.5 font-sans text-slate-700">Applicable Mandi MSP Rate</td>
                      <td className="p-2.5 text-right font-bold text-slate-900">₹2,400.00 / Quintal</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Total Payout Highlight */}
            <div className="p-4 bg-[#F3F6F1] rounded-2xl border border-[#A3B18A]/50 flex items-center justify-between">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-wider text-[#1B4332]">
                  Total Payable Procurement Amount
                </p>
                <p className="text-2xl font-black text-[#1B4332] mt-0.5">
                  ₹{payout.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                </p>
                <p className="text-[10px] text-[#2D6A4F] mt-0.5">
                  Zero commission deducted (M.P. Farmers Protection Guarantee)
                </p>
              </div>
              <Banknote className="w-10 h-10 text-[#2D6A4F] opacity-75" />
            </div>

            {/* DBT Bank Transfer Status */}
            <div className="p-3.5 rounded-2xl bg-[#F3F6F1]/70 border border-slate-200/80 text-[11px] space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-800 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-[#2D6A4F]" />
                  Direct Benefit Transfer (DBT) Status
                </span>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#1B4332] text-white uppercase tracking-wider">
                  {booking.paymentStatus.replace(/_/g, ' ')}
                </span>
              </div>
              <p className="text-slate-600">
                Amount routed to Aadhaar-linked Bank A/C ending in <strong>****{booking.bankAccountLast4 || '4589'}</strong>
              </p>
              <p className="text-slate-500 font-mono text-[10px]">
                DBT Reference UTR: <strong>{booking.utrNumber || 'MPDBT2026090799428'}</strong>
              </p>
            </div>

            {/* Digital Seals & Signatures */}
            <div className="pt-3 border-t border-slate-200 flex items-center justify-between text-[10px] text-slate-500">
              <div>
                <p className="font-bold text-slate-700">Weighbridge Operator ID: MP-OPR-412</p>
                <p>Digital Cryptographic Hash: 8f4a9b21e0</p>
              </div>
              <div className="text-right">
                <div className="inline-flex items-center gap-1 text-[#1B4332] font-bold border border-[#A3B18A]/60 bg-[#D4E09B]/30 px-2 py-0.5 rounded-md">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>Digitally Verified</span>
                </div>
                <p className="mt-0.5">Secretary, Mandi Samiti</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
