require('dotenv').config();
const express = require('express');
const cors = require('cors');
const axios = require('axios');
const crypto = require('crypto');

const app = express();
app.use(cors());
app.use(express.json());

const BINANCE_API_KEY = process.env.BINANCE_API_KEY;
const BINANCE_SECRET_KEY = process.env.BINANCE_SECRET_KEY;

// Keep track of redeemed TXIDs in memory (in production, use a real DB!)
const redeemedTxIds = new Set();

const PLANS = {
    'weekly': 0.29,
    'monthly': 0.49,
    'quarterly': 1.50,
    'yearly': 5.00
};

app.post('/verify-transaction', async (req, res) => {
    try {
        const { txId, planId } = req.body;

        if (!txId || !planId || !PLANS[planId]) {
            return res.status(400).json({ success: false, error: 'Invalid request parameters.' });
        }

        if (redeemedTxIds.has(txId)) {
            return res.status(400).json({ success: false, error: 'Transaction ID already redeemed.' });
        }

        const expectedAmount = PLANS[planId];
        const timestamp = Date.now();
        
        // Query Binance Pay History
        // Endpoint: GET /sapi/v1/pay/transactions
        const queryString = `timestamp=${timestamp}`;
        const signature = crypto
            .createHmac('sha256', BINANCE_SECRET_KEY)
            .update(queryString)
            .digest('hex');

        const response = await axios.get(`https://api.binance.com/sapi/v1/pay/transactions?${queryString}&signature=${signature}`, {
            headers: {
                'X-MBX-APIKEY': BINANCE_API_KEY
            }
        });

        const transactions = response.data.data || [];
        
        // Find the transaction by orderId (which acts as TxID for Binance Pay)
        // Note: Users might input the 'orderId' or 'trxId'. We should check both if possible.
        const tx = transactions.find(t => t.orderId === txId || t.trxId === txId);

        if (!tx) {
            return res.status(404).json({ success: false, error: 'Transaction not found. Please wait a moment and try again.' });
        }

        // Verify it is an incoming transaction (funds received)
        // Usually funds received have positive amount, or specific payer info
        // Let's verify amount and currency (USDT or BUSD typically, but let's check amount first)
        const amountPaid = parseFloat(tx.amount);
        if (amountPaid < expectedAmount) {
            return res.status(400).json({ success: false, error: 'Transaction amount is less than required for this plan.' });
        }

        // Mark as redeemed
        redeemedTxIds.add(txId);

        return res.json({
            success: true,
            message: 'Transaction verified successfully!',
            planId: planId,
            durationDays: getDurationDays(planId)
        });

    } catch (error) {
        console.error('Binance API Error:', error.response ? error.response.data : error.message);
        return res.status(500).json({ success: false, error: 'Failed to verify transaction with Binance.' });
    }
});

function getDurationDays(planId) {
    switch (planId) {
        case 'weekly': return 7;
        case 'monthly': return 30;
        case 'quarterly': return 90;
        case 'yearly': return 365;
        default: return 0;
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Binance Verification Server running on port ${PORT}`);
});
