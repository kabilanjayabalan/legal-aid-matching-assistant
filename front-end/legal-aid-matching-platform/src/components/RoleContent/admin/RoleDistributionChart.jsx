import React, { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { getUserRoleAnalytics } from '../../../services/analytics';

const COLORS = ['#2563eb', '#059669', '#dc2626', '#7c3aed'];

const RoleDistributionChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalUsers, setTotalUsers] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getUserRoleAnalytics();
        setTotalUsers(res.data.totalUsers);

        const chartData = [
          { name: 'Citizens', value: res.data.citizens },
          { name: 'Lawyers', value: res.data.lawyers },
          { name: 'NGOs', value: res.data.ngos },
          { name: 'Admins', value: res.data.admins }
        ];

        setData(chartData);
      } catch (err) {
        console.error("Error fetching user role analytics:", err);
        setError("Failed to load user statistics");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return <div className="p-4 text-center">Loading chart data...</div>;
  }

  if (error) {
    return <div className="p-4 text-center text-red-500">{error}</div>;
  }

  if (!data.length) {
    return (
      <div className="p-4 text-center text-gray-500">
        No user data available to display.
      </div>
    );
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-md">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">
        User Role Distribution
      </h3>

      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) =>
                `${name} ${(percent * 100).toFixed(0)}%`
              }
              outerRadius={80}
              dataKey="value"
            >
              {data.map((_, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={COLORS[index % COLORS.length]}
                />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <p className="text-sm text-gray-500 mt-2">
        Breakdown of platform users by their registered roles-
        <span className="font-semibold text-gray-700">
          {" "}Total Users: {totalUsers}
        </span>
      </p>
    </div>
  );
};

export default RoleDistributionChart;
