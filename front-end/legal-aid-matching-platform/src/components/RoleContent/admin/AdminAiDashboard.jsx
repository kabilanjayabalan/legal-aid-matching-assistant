import React from "react";
import { Sparkles, Activity, Database, AlertTriangle, CheckCircle, Zap } from "lucide-react";

export default function AdminAiDashboard() {
  // Mock data for AI Limits
  const aiStats = {
    model: "llama-3.1-8b-instant",
    provider: "Groq",
    monthlyLimit: 1000000,
    currentUsage: 425310,
    tokensPerMinuteLimit: 6000,
    requestsPerMinuteLimit: 30,
    status: "Healthy",
  };

  const usagePercentage = (aiStats.currentUsage / aiStats.monthlyLimit) * 100;

  return (
    <div className="p-6">
      <div className="flex items-center gap-3 mb-8">
        <div className="p-3 bg-blue-100 text-blue-700 rounded-lg">
          <Sparkles size={24} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-800">AI Model & Credit Limits</h1>
          <p className="text-gray-500">Monitor usage and limits for the integrated AI assistant</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {/* Status Card */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex justify-between items-start mb-4">
            <h3 className="text-gray-500 font-medium">Service Status</h3>
            <Activity className="text-green-500" />
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="text-green-500" size={20} />
            <span className="text-2xl font-bold text-gray-800">{aiStats.status}</span>
          </div>
          <p className="text-sm text-gray-400 mt-2">Provider: {aiStats.provider}</p>
        </div>

        {/* Model Card */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex justify-between items-start mb-4">
            <h3 className="text-gray-500 font-medium">Active Model</h3>
            <Database className="text-blue-500" />
          </div>
          <h2 className="text-xl font-bold text-gray-800 truncate" title={aiStats.model}>
            {aiStats.model}
          </h2>
          <p className="text-sm text-gray-400 mt-2">High-speed inference model</p>
        </div>

        {/* Rate Limits */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex justify-between items-start mb-4">
            <h3 className="text-gray-500 font-medium">Rate Limits</h3>
            <Zap className="text-yellow-500" />
          </div>
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-gray-600">Requests / Min:</span>
            <span className="font-bold text-gray-800">{aiStats.requestsPerMinuteLimit}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-600">Tokens / Min:</span>
            <span className="font-bold text-gray-800">{aiStats.tokensPerMinuteLimit.toLocaleString()}</span>
          </div>
        </div>
      </div>

      {/* Main Usage Card */}
      <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-bold text-gray-800">Monthly Credit Usage</h2>
          {usagePercentage > 80 && (
            <span className="flex items-center gap-1 text-sm font-medium text-orange-600 bg-orange-100 px-3 py-1 rounded-full">
              <AlertTriangle size={14} />
              Approaching Limit
            </span>
          )}
        </div>

        <div className="mb-4 flex justify-between items-end">
          <div>
            <p className="text-4xl font-bold text-gray-800">
              {aiStats.currentUsage.toLocaleString()}
            </p>
            <p className="text-gray-500">tokens used this month</p>
          </div>
          <div className="text-right">
            <p className="text-xl font-semibold text-gray-600">
              {aiStats.monthlyLimit.toLocaleString()}
            </p>
            <p className="text-sm text-gray-400">total monthly limit</p>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-gray-100 rounded-full h-4 mb-2 overflow-hidden">
          <div
            className={`h-4 rounded-full transition-all duration-1000 ${
              usagePercentage > 90 ? "bg-red-500" : usagePercentage > 75 ? "bg-orange-500" : "bg-blue-500"
            }`}
            style={{ width: `${usagePercentage}%` }}
          ></div>
        </div>
        
        <div className="flex justify-between text-sm">
          <span className="font-medium text-gray-500">{usagePercentage.toFixed(1)}% used</span>
          <span className="text-gray-400">Resets on the 1st of next month</span>
        </div>
      </div>
    </div>
  );
}
