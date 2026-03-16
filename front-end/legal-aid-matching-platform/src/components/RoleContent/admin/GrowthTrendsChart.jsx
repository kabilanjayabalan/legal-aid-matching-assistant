import React, { useEffect, useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { getTrendsAnalytics } from "../../../services/analytics";

export default function GrowthTrendsChart() {
  const [data, setData] = useState([]);
  const [range, setRange] = useState("monthly"); // default

  useEffect(() => {
    fetchTrends();
  }, [range]);

  const fetchTrends = async () => {
    try {
      const res = await getTrendsAnalytics(range);
      setData(transformTrendData(res.data));
    } catch (err) {
      console.error("Failed to fetch trends", err);
    }
  };


  const transformTrendData = (apiData) => {
  const map = {};

  const merge = (list, key) => {
    list.forEach((item) => {
      const rawTime = item.time; // MUST stay unique

      if (!map[rawTime]) {
        map[rawTime] = {
          time: rawTime,
          label: formatTime(rawTime),
        };
      }

      map[rawTime][key] = item.count;
    });
  };

  merge(apiData.users || [], "users");
  merge(apiData.cases || [], "cases");
  merge(apiData.matches || [], "matches");

  return Object.values(map)
    .sort((a, b) => new Date(a.time) - new Date(b.time))
    .map((row) => ({
      time: row.time,
      label: row.label,
      users: row.users || 0,
      cases: row.cases || 0,
      matches: row.matches || 0,
    }));
};


const formatTime = (iso) => {
  const date = new Date(iso);

  if (range === "daily") {
    return date.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
    });
  }

  if (range === "monthly") {
    return date.toLocaleDateString("en-IN", {
      month: "short",
      year: "numeric",
    });
  }

  return date.getFullYear().toString(); // yearly
};


  return (
    <div className="bg-white p-6 rounded-xl shadow-md">
      <h3 className="text-lg font-semibold text-gray-800 mb-4">
        Platform Growth Trends
      </h3>

      {/* RANGE SELECTOR (moved outside chart height) */}
      <div className="flex gap-2 mb-4">
        {["daily", "monthly", "yearly"].map((r) => (
          <button
            key={r}
            onClick={() => setRange(r)}
            className={`px-3 py-1 rounded text-sm transition ${
              range === r ? "bg-blue-950 text-white" : "bg-gray-100"
            }`}
          >
            {r.charAt(0).toUpperCase() + r.slice(1)}
          </button>
        ))}
      </div>

      {/* CHART */}
      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={data}
            margin={{ top: 10, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} />

            <XAxis
              dataKey="label"
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#6b7280", fontSize: 12 }}
            />

            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#6b7280", fontSize: 12 }}
            />

            <Tooltip
              contentStyle={{
                backgroundColor: "#fff",
                borderRadius: "8px",
                border: "none",
                boxShadow:
                  "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
              }}
              itemStyle={{ fontSize: "12px" }}
            />

            {/* LEGEND FIXED */}
            <Legend
              verticalAlign="top"
              align="center"
              height={36}
              wrapperStyle={{ fontSize: "12px" }}
            />

            <Line
              type="monotone"
              dataKey="users"
              name="New Users"
              stroke="#3b82f6"
              strokeWidth={3}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="cases"
              name="New Cases"
              stroke="#f97316"
              strokeWidth={3}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="matches"
              name="Successful Matches"
              stroke="#10b981"
              strokeWidth={3}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
