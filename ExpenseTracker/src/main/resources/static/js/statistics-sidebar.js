function toggleStatsSidebar() {
    document.getElementById('statisticsSidebar').classList.toggle('open');
}

document.addEventListener("DOMContentLoaded", function () {
    const ctx = document.getElementById('expenseChart')?.getContext('2d');
    if (!ctx) return;

    fetch('/statistics-data')
        .then(response => response.json())
        .then(data => {
            console.log("Fetched Data from /statistics-data:", data);
            const labels = Object.keys(data);
            const values = Object.values(data);
            const colors = [
                '#4E79A7', '#F28E2B', '#E15759', '#76B7B2',
                '#59A14F', '#EDC949', '#AF7AA1', '#FF9DA7',
                '#9C755F', '#BAB0AB'
            ];

            if (window.expenseChartInstance) {
                window.expenseChartInstance.destroy();
            }

            window.expenseChartInstance = new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: colors.slice(0, labels.length)
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                color: '#232526',
                                font: { size: 13 }
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function (context) {
                                    const label = context.label || '';
                                    const value = context.raw || 0;
                                    return `${label}: ₹${value.toFixed(2)}`;
                                }
                            }
                        }
                    }
                }
            });
        })
        .catch(error => console.error('Chart loading error:', error));

    fetch('/weekly-expense-data')
        .then(response => response.json())
        .then(data => {
            const labels = Object.keys(data); // ["1", "7", "14", "21", "31"]
            const values = Object.values(data);

            const ctx = document.getElementById('weeklyExpenseChart').getContext('2d');

            // Add dynamic title based on current month
            const now = new Date();
            const monthName = now.toLocaleString('default', { month: 'long' });
            const titleText = `Expenses in ${monthName}`;

            if (window.weeklyChartInstance) {
                window.weeklyChartInstance.destroy();
            }

            window.weeklyChartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Total ₹',
                        data: values,
                        fill: false,
                        borderColor: '#4E79A7',
                        backgroundColor: '#4E79A7',
                        tension: 0.3
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: titleText,
                            font: {
                                size: 16,
                                weight: 'bold'
                            },
                            color: '#232526'
                        },
                        legend: {
                            display: false
                        },
                        tooltip: {
                            callbacks: {
                                label: context => `₹${context.raw.toFixed(2)}`
                            }
                        }
                    },
                    scales: {
                        x: {
                            title: {
                                display: true,
                                text: 'Day of Month'
                            }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: {
                                precision: 0,
                                callback: value => `₹${value}`
                            }
                        }
                    }
                }
            });
        });
});
document.addEventListener('click', function (event) {
    const sidebar = document.getElementById("statisticsSidebar");
    const toggleButton = document.querySelector('a[onclick="toggleStatsSidebar()"]');

    const isSidebarOpen = sidebar && sidebar.classList.contains('open');
    const clickedInsideSidebar = sidebar && sidebar.contains(event.target);
    const clickedToggleButton = toggleButton && toggleButton.contains(event.target);

    if (isSidebarOpen && !clickedInsideSidebar && !clickedToggleButton) {
        sidebar.classList.remove('open');
    }
});