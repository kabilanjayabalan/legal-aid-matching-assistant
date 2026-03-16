import { Gavel } from 'lucide-react';

const LegalLoader = () => {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-slate-50">
      <div className="flex flex-col items-center justify-center space-y-4">
        <div className="relative">
          {/* The Gavel Icon */}
          <Gavel
            className="w-12 h-12 text-slate-800"
            style={{
              animation: 'hammerStrike 1s ease-in-out infinite',
              transformOrigin: '25% 75%'
            }}
          />
          {/* Static Base Block */}
          <div className="w-16 h-2 bg-blue-950 rounded-full mt-[-4px]" />
        </div>
      </div>

      <style>{`
        @keyframes hammerStrike {
          0%, 100% {
            transform: rotate(25deg);
          }
          50% {
            transform: rotate(-15deg);
          }
        }
      `}</style>
    </div>
  );
};

export default LegalLoader;
