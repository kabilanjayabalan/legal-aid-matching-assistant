import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { getCasesAnalytics } from '../../../services/analytics'; // adjust path if needed

const CATEGORY_LABELS = {
  CIVIL: 'Civil',
  CRIMINAL: 'Criminal',
  FAMILY: 'Family',
  PROPERTY: 'Property',
  EMPLOYMENT: 'Employment'
};

const CaseCategoriesChart = () => {
  const [data, setData] = useState([]);
  const [totalCases, setTotalCases] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getCasesAnalytics();
        setTotalCases(res.data.totalCases);

        const casesByCategory = res.data.casesByCategory;

        // Transform backend map → recharts array
        const chartData = Object.entries(casesByCategory).map(
          ([key, value]) => ({
            name: CATEGORY_LABELS[key] || key,
            OPEN: value.OPEN ?? 0,
            IN_PROGRESS: value.IN_PROGRESS ?? 0,
            CLOSED: value.CLOSED ?? 0,
            TOTAL: value.TOTAL ?? 0,
          })
        );

        setData(chartData);
      } catch (err) {
        console.error('Error fetching case stats:', err);
        setError('Failed to load case statistics');
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
        No case data available to display.
      </div>
    );
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-md">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Case Categories Distribution</h3>
      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={data}
            layout="vertical" // Horizontal bars for better readability on mobile
            margin={{
              top: 5,
              right: 30,
              left: 15, // Increased left margin even more to accommodate "EMPLOYMENT"
              bottom: 0,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" />
            <YAxis 
              dataKey="name" 
              type="category" 
              width={110} // Increased width for Y-axis labels
              tick={{ fontSize: 12 }} 
            />
            <Tooltip />
            <Legend />
            <Bar dataKey="TOTAL" fill="#1e3a8a" name="Number of Cases" radius={[0, 4, 4, 0]} />
            <Bar dataKey="OPEN" stackId="a" fill="#b025eb" name="Open" />
            <Bar dataKey="IN_PROGRESS" stackId="a" fill="#f59e0b" name="In Progress" />
            <Bar dataKey="CLOSED" stackId="a" fill="#16a34a" name="Closed" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <p className="text-sm text-gray-600 mt-4 text-center">
        Showing distribution across{" "}
        <span className="font-medium">
          {totalCases}
        </span>{" "}
        total cases
      </p>
    </div>
  );
};

export default CaseCategoriesChart;
