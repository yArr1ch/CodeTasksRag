#!/bin/sh
set -eu

topic_name="task-generation-requested"
queue_name="task-generation-requests"
dead_letter_queue_name="task-generation-requests-dlq"

topic_arn="$(awslocal sns create-topic \
  --name "$topic_name" \
  --query TopicArn \
  --output text)"

dead_letter_queue_url="$(awslocal sqs create-queue \
  --queue-name "$dead_letter_queue_name" \
  --attributes '{"VisibilityTimeout":"900","ReceiveMessageWaitTimeSeconds":"20"}' \
  --query QueueUrl \
  --output text)"

dead_letter_queue_arn="$(awslocal sqs get-queue-attributes \
  --queue-url "$dead_letter_queue_url" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

queue_url="$(awslocal sqs create-queue \
  --queue-name "$queue_name" \
  --attributes '{"VisibilityTimeout":"900","ReceiveMessageWaitTimeSeconds":"20"}' \
  --query QueueUrl \
  --output text)"

queue_arn="$(awslocal sqs get-queue-attributes \
  --queue-url "$queue_url" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

redrive_policy="$(printf '{"deadLetterTargetArn":"%s","maxReceiveCount":"3"}' "$dead_letter_queue_arn")"
escaped_redrive_policy="$(printf '%s' "$redrive_policy" | sed 's/"/\\"/g')"
awslocal sqs set-queue-attributes \
  --queue-url "$queue_url" \
  --attributes "{\"RedrivePolicy\":\"$escaped_redrive_policy\"}" \
  >/dev/null

queue_policy="$(printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":"sqs:SendMessage","Resource":"%s","Condition":{"ArnEquals":{"aws:SourceArn":"%s"}}}]}' "$queue_arn" "$topic_arn")"
escaped_queue_policy="$(printf '%s' "$queue_policy" | sed 's/"/\\"/g')"
awslocal sqs set-queue-attributes \
  --queue-url "$queue_url" \
  --attributes "{\"Policy\":\"$escaped_queue_policy\"}" \
  >/dev/null

subscription_arn="$(awslocal sns subscribe \
  --topic-arn "$topic_arn" \
  --protocol sqs \
  --notification-endpoint "$queue_arn" \
  --query SubscriptionArn \
  --output text)"

awslocal sns set-subscription-attributes \
  --subscription-arn "$subscription_arn" \
  --attribute-name RawMessageDelivery \
  --attribute-value true \
  >/dev/null

echo "Configured SNS topic $topic_name -> SQS queue $queue_name"
